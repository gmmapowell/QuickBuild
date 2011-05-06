package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.git.GitHelper;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.graphs.Link;
import com.gmmapowell.graphs.Node;
import com.gmmapowell.graphs.NodeWalker;
import com.gmmapowell.quickbuild.build.java.JUnitFailure;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.DateUtils;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;

/* Somewhere deep inside here, there is structure waiting to break out.
 * I think there are really 4 separate functions for this class:
 * 
 *   * Managing the build state (config, resources, etc)
 *   * Managing the build order (strats, tactics, floating etc)
 *   * Managing the build dependencies
 *   * Actually handling all the dirtyness of execution
 *   
 * But I can't see how to disentangle them.
 */
public class BuildContext implements ResourceListener {
	public class DeferredTactic {
		final String id;
		final Tactic bc;

		public DeferredTactic(String id, Tactic bc) {
			this.id = id;
			this.bc = bc;
		}
	}

	public static class ComparisonResource extends SolidResource {
		private final String comparison;

		public ComparisonResource(String from) {
			super(null, new File(FileUtils.getCurrentDir(), "unused"));
			this.comparison = from;
		}

		@Override
		public Strategem getBuiltBy() {
			throw new UtilException("Not implemented");
		}

		@Override
		public File getPath() {
			throw new UtilException("Not implemented");
		}

		@Override
		public String compareAs() {
			return comparison;
		}

	}

	public class Notification {
		private final Class<? extends BuildResource> cls;
		private final Nature nature;

		public Notification(Class<? extends BuildResource> cls, Nature nature) {
			this.cls = cls;
			this.nature = nature;
		}

		public void dispatch(BuildResource br)
		{
			if (cls.isAssignableFrom(br.getClass()))
				nature.resourceAvailable(br);
		}
	}

	public static class NeedsResource {
		final StrategemResource node;
		final BuildResource br;

		public NeedsResource(StrategemResource node, BuildResource br) {
			this.node = node;
			this.br = br;
		}

	}


	private final Config conf;
	private final Map<String, BuildResource> availableResources = new TreeMap<String, BuildResource>();
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final List<JUnitFailure> failures = new ArrayList<JUnitFailure>();
	private final File dependencyFile;
	private final File buildOrderFile;
	private int strategemToExecute = -1;
	private int targetFailures;
	private Date buildStarted;
	private int totalErrors;
	private boolean buildBroken;
	private int projectsWithTestFailures;
	private List<StrategemResource> strats = new ArrayList<StrategemResource>();
	private StrategemResource currentStrat;
	private Iterator<? extends Tactic> currentCommands;
	private int currentStrategemCommandNo;
	private boolean moveOn = true;
	private Map<Class<?>, Object> natures = new HashMap<Class<?>, Object>();
	private final List<Notification> notifications = new ArrayList<BuildContext.Notification>();
	private Tactic repeat;
	private boolean buildAll;
	private final List<Pattern> showArgsFor = new ArrayList<Pattern>();
	private final List<Pattern> showDebugFor = new ArrayList<Pattern>();
	private final List<DeferredTactic> deferred = new ArrayList<DeferredTactic>();
	private String currentId;

	public BuildContext(Config conf, ConfigFactory configFactory, boolean buildAll, List<String> showArgsFor, List<String> showDebugFor) {
		this.conf = conf;
		this.buildAll = buildAll;
		for (String s : showArgsFor)
			this.showArgsFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (String s : showDebugFor)
			this.showDebugFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (Class<?> n : configFactory.registeredNatures())
			registerNature(n);
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
		buildOrderFile = new File(conf.getCacheDir(), "buildOrder.xml");
		for (Strategem s : conf.getStrategems())
			strats.add(new StrategemResource(s));
	}
	
	public void configure()
	{
		conf.tellMeAboutInitialResources(this);
		List<NeedsResource> needs = new ArrayList<NeedsResource>();
		Set<BuildResource> offered = new HashSet<BuildResource>();
		offered.addAll(availableResources.values());
		for (StrategemResource node : strats)
		{
			Strategem s = node.getBuiltBy();
//			System.out.println("Configuring " + s);
			dependencies.ensure(node);
			
			// TODO: understand how this should work for these different cases
			for (BuildResource br : s.needsResources())
			{
				needs.add(new NeedsResource(node, br));
			}			

			for (BuildResource br : s.providesResources())
			{
				// conf.willBuild(br);
				dependencies.ensure(br);
				offered.add(br);
				resourceAvailable(br);
			}
			
			for (BuildResource br : s.buildsResources())
			{
				conf.willBuild(br);
				offered.add(br);
				dependencies.ensure(br);
			}
		}
		
		for (NeedsResource nr : needs)
		{
			if (offered.contains(nr.br))
				dependencies.ensureLink(nr.node, nr.br);
			else if (nr.br instanceof PendingResource)
			{
				BuildResource uniq = null;
				Pattern p = Pattern.compile(".*"+nr.br.compareAs().toLowerCase()+".*");
				for (BuildResource br : offered)
				{
					if (p.matcher(br.compareAs().toLowerCase()).matches())
					{
						if (uniq != null)
							throw new QuickBuildException("Cannot resolve comparison: " + nr.br.compareAs() + " matches at least " + uniq.compareAs() + " and" + br.compareAs());
						uniq = br;
					}
				}
				if (uniq == null)
					throw new QuickBuildException("Could not find any dependency that matched " + nr.br.compareAs() +": have " + offered);
				dependencies.ensureLink(nr.node, uniq);
			}
			else
				throw new QuickBuildException("Could not resolve need " + nr.br);
		}
	}
	
	public Iterable<BuildResource> getResources(Class<? extends BuildResource> ofType)
	{
		List<BuildResource> ret = new ArrayList<BuildResource>();
		for (BuildResource br : availableResources.values())
			if (ofType.isInstance(br))
				ret.add(br);
		return ret;
	}
	 
	private void moveUp(Strategem current, Strategem required) {
		for (int idx=strategemToExecute+1;idx<strats.size();idx++)
		{
			if (strats.get(idx).getBuiltBy() == required)
			{
				StrategemResource sr = strats.remove(idx);
				strats.add(strategemToExecute, sr);
				currentCommands = null;
				moveOn = false;
				repeat = null;
				return;
			}
		}
	}

	public void loadCache() {
		loadBuildOrderCache();
		loadDependencyCache();
	}
	
	private void loadBuildOrderCache() {
		if (!buildOrderFile.canRead())
		{
			buildAll = true;
			return;
		}
		final XML input = XML.fromFile(buildOrderFile);
		int moveTo = 0;
		loop:
		for (XMLElement e : input.top().elementChildren())
		{
			String from = e.get("strategem");
			for (int i=moveTo;i<strats.size();i++)
			{
				StrategemResource node = strats.get(i);
				if (node.compareAs().equals(from))
				{
					if (i != moveTo)
					{
						strats.remove(i);
						strats.add(moveTo, node);
					}
					moveTo++;
					continue loop;
				}
			}
			buildOrderFile.delete();
			throw new QuickBuildCacheException("Did not find any build commands for " + from);
		}
	}

	public void loadDependencyCache()
	{
		if (!dependencyFile.canRead())
		{
			buildAll = true;
			return;
		}
		try
		{
			final XML input = XML.fromFile(dependencyFile);
			for (XMLElement e : input.top().elementChildren())
			{
				String from = e.get("from");
				Node<BuildResource> target = dependencies.find(new ComparisonResource(from));
				for (XMLElement r : e.elementChildren())
				{
					String resource = r.get("resource");
					Node<BuildResource> source = dependencies.find(new ComparisonResource(resource));
//					System.out.println(target + " <= " + source);
					dependencies.ensureLink(target.getEntry(), source.getEntry());
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			// TODO: we should clear all the links out, but we need to keep the nodes
			dependencyFile.delete();
			throw new QuickBuildCacheException("Could not decipher the dependency cache");
		}
	}

	public void saveDependencies() {
		final XML output = XML.create("1.0", "Dependencies");
		dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {
			@Override
			public void present(Node<BuildResource> node) {
				XMLElement dep = output.addElement("Dependency");
				dep.setAttribute("from", node.getEntry().compareAs());
				for (Link<BuildResource> l : node.linksFrom())
				{
					XMLElement ref = dep.addElement("References");
					BuildResource to = l.getTo();
					ref.setAttribute("resource", to.compareAs());
				}
			}

		});
		FileUtils.assertDirectory(dependencyFile.getParentFile());
		output.write(dependencyFile);
	}


	public void saveBuildOrder() {
		final XML output = XML.create("1.0", "BuildOrder");
		for (StrategemResource s : strats)
		{
			XMLElement item = output.addElement("BuildItem");
			item.setAttribute("strategem", s.compareAs());
		}
		FileUtils.assertDirectory(buildOrderFile.getParentFile());
		output.write(buildOrderFile);
	}

	public BuildStatus execute(Tactic bc) {
		// Check all the declared dependencies for the Strategem are there
		for (BuildResource br : bc.belongsTo().needsResources())
			if (!isResourceAvailable(br))
			{
				System.out.println(bc.belongsTo() + " needs resource " + br + " which is not yet available in " + availableResources);
				if (strategemToExecute == strats.size()-1)
				{
					System.out.println(bc.belongsTo() + " depends on resource " + br + " but nobody left to build it");
					return BuildStatus.BROKEN;
				}
				else if (br.getBuiltBy() != null)
				{
					moveUp(bc.belongsTo(), br.getBuiltBy());
					if (bc.belongsTo() == strats.get(strategemToExecute))
					{
						System.out.println(bc.belongsTo() + " depends on resource " + br + " but its owner " + br.getBuiltBy() + " could not be moved up ... circular dependency?");
						return BuildStatus.BROKEN;
					}
				}
				else
				{
					// This is in fact a valid broken case, but it's complicated.
					// Basically, it involves depending on a pending resource, so you need to figure out (from willBuild) who will build that
					// and then move them up here.
					
					// For now, I'm going to let Java broken builds do the work ...
					
					if (br instanceof PendingResource)
					{
						Strategem builder = findBuilderFor((PendingResource) br);
						if (builder != null)
							moveUp(bc.belongsTo(), builder);
						else
						{
							System.out.println("Depending on " + br + " but could not find a builder");
							return BuildStatus.BROKEN;
						}
					}
					/*
					System.out.println("Moving " + bc.belongsTo() + " after " + strats.get(strategemToExecute+1).getBuiltBy());
					strats.add(strategemToExecute+2, currentStrat);
					strats.remove(strategemToExecute);

					// TODO: this logic should be in the "processing" of RETRY.
					// We absolutely need a separate "BuildOrder and Dependencies" module, which can handle all this stuff
					// (i.e. this whole big 50-line if clause should be in it).
					currentCommands = null;
					moveOn = false;
					repeat = null;

					// It's complicated, but we sort-of-want to add the constraint that we sort-of-depend on this (i.e. want to be after it)
					// but we don't really depend on it.  But I think that coming through here is kind of broken anyway (basically we're looking 
					// for a pending resource and we're trying to drift downwards
					return BuildStatus.RETRY;
					*/
				}
			}

		// figure out if we need to build this
		boolean doit = true;
		boolean floatMe = dependencyFloat(bc);
		String id = (strategemToExecute+1)+"."+currentStrategemCommandNo;
		if (currentId != null)
		{
			id = currentId;
			System.out.print("+ ");
		}
		else if (currentStrat.isClean())
		{
			doit = false;
			System.out.print("  ");
		}
		else if (floatMe)
		{
			System.out.print("- ");
			deferred.add(new DeferredTactic(id, bc));
		}
		else
			System.out.print("* ");
		System.out.println(id + ": " + bc);
		if (floatMe)
			return BuildStatus.DEFERRED;
		if (!doit)
			return BuildStatus.CLEAN;

		// Record when first build started
		if (buildStarted == null)
			buildStarted = new Date();
		BuildStatus ret = BuildStatus.BROKEN;
		try
		{
			ret = bc.execute(this, showArgs(bc), showDebug(bc));
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace(System.out);
		}
		if (ret.needsRebuild())
			getGitCacheFile(stratFor(bc)).delete();
		
		// Test the contract when the strategem comes to an end
		if (ret.builtResources() && currentCommands != null && !currentCommands.hasNext())
		{
			List<BuildResource> fails = new ArrayList<BuildResource>();
			for (BuildResource br : bc.belongsTo().buildsResources())
				if (!isResourceAvailable(br))
					fails.add(br);
			if (!fails.isEmpty())
			{
				System.out.println("The strategem " + bc.belongsTo() + " failed in its contract to build " + fails);
				// This code should be abstracted out too ... I think we need another wrapper layer.
				getGitCacheFile(stratFor(bc)).delete();
				return BuildStatus.BROKEN;
			}
		}
		return ret;
	}

	private Strategem findBuilderFor(PendingResource wanted) {
		// I think this code is now duplicated three times!
		Pattern p = Pattern.compile(".*" + wanted.compareAs().toLowerCase()+".*");
		for (int i=strategemToExecute+1;i<strats.size();i++)
			for (BuildResource br : strats.get(i).getBuiltBy().buildsResources())
				if (p.matcher(br.compareAs().toLowerCase()).matches())
					return strats.get(i).getBuiltBy();
		return null;
	}

	private StrategemResource stratFor(Tactic bc) {
		return new StrategemResource(bc.belongsTo());
	}

	private boolean dependencyFloat(Tactic bc) {
		if (!(bc instanceof DependencyFloat))
			return false;
		ResourcePacket needsAdditionalBuiltResources = ((DependencyFloat)bc).needsAdditionalBuiltResources();
		if (needsAdditionalBuiltResources == null)
			return false;
		for (BuildResource br : needsAdditionalBuiltResources)
			if (getPendingResourceIfAvailable((PendingResource) br) == null)
				return true;
		return false;
	}

	private boolean showArgs(Tactic bc) {
		for (Pattern p : showArgsFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	private boolean showDebug(Tactic bc) {
		for (Pattern p : showDebugFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	public void junitFailure(JUnitRunCommand cmd, String stdout, String stderr) {
		JUnitFailure failure = new JUnitFailure(cmd, stdout, stderr);
		failures.add(failure);
		projectsWithTestFailures++;
	}

	public void showAnyErrors() {
		for (JUnitFailure failure : failures)
			failure.show();
		if (buildBroken)
		{
			System.out.println("!!!! BUILD FAILED !!!!");
			System.exit(1);
		}
		else if (buildStarted == null) {
			System.out.println("Nothing done.");
		} else
		{
			System.out.print(">> Build completed in ");
			System.out.print(DateUtils.elapsedTime(buildStarted, new Date(), DateUtils.Format.hhmmss3));
			if (projectsWithTestFailures > 0)
				System.out.print(" " + projectsWithTestFailures + " projects had test failures");
			if (totalErrors > 0)
				System.out.print(" " + totalErrors + " total build commands failed (including retries)");
			System.out.println();
		}
	}

	public Tactic next() {
		for (;;)
		{
			if (repeat != null)
				return repeat;
			currentId = null;
			
			if (currentCommands != null && currentCommands.hasNext())
			{
				currentStrategemCommandNo++;
				repeat = currentCommands.next();
				return repeat;
			}
		
			if (moveOn)
			{
				if (currentStrat != null && currentStrat.isClean())
				{
					for (BuildResource br : currentStrat.getBuiltBy().buildsResources())
					{
						resourceAvailable(br);
					}
				}
				strategemToExecute++; 
			}

			for (DeferredTactic d : deferred)
				if (!dependencyFloat(d.bc))
				{
					currentId = d.id;
					repeat = d.bc;
					deferred.remove(d);
					return repeat;
				}
			
			if (strategemToExecute >= strats.size())
			{
				if (deferred.size() > 0)
				{
					for (DeferredTactic dt : deferred)
					{
						System.out.println("! " + dt.bc.toString());
						getGitCacheFile(stratFor(dt.bc)).delete();
					}
					throw new QuickBuildException("At end of processing, some targets were not built");
				}
				return null;
			}
	
			currentStrat = strats.get(strategemToExecute);
			if (currentStrat.getBuiltBy() instanceof FloatToEnd)
				currentStrat = tryToFloatDownwards();
			figureDirtyness(currentStrat, buildAll);
			currentCommands = currentStrat.getBuiltBy().tactics().iterator();
			currentStrategemCommandNo = 0;
		}
	}

	private StrategemResource tryToFloatDownwards() {
		// So, we've found that this one would like to float down.
		// It can't go below anyone who wants to go down more, or
		// anyone that it's dependent on (transitively), but it should be able to move
		// them down too.
		// I'm leaving that later case for when it arises.
		
		Strategem me = currentStrat.getBuiltBy();
		int pri = ((FloatToEnd)me).priority();
		int curpos;
		for (curpos = strategemToExecute+1;curpos < strats.size();curpos++)
		{
			if (dependencies.hasLink(strats.get(curpos), currentStrat))
				break;
			Strategem compareTo = strats.get(curpos).getBuiltBy();
			if (!(compareTo instanceof FloatToEnd))
				continue;
			if (pri <= ((FloatToEnd)compareTo).priority())
				break;
		}
		if (curpos != strategemToExecute+1)
		{
			strats.add(curpos, currentStrat);
			strats.remove(strategemToExecute);
		}
		return strats.get(strategemToExecute);
	}

	public void advance() {
		targetFailures = 0;
		repeat = null;
		moveOn = true;
	}

	public void buildFail(BuildStatus outcome) {
		totalErrors++;
		if (outcome.isBroken())
			buildBroken = true;
	}

	public void tryAgain() {
		if (++targetFailures >= 5)
			throw new UtilException("The strategy " + currentStrat + " failed 3 times in a row");
	}

	public File getPath(String name) {
		return conf.getPath(name);
	}

	public void clearCache() {
		dependencies.clear();
	}

	public String printableDependencyGraph() {
		return dependencies.toString();
	}

	@Override
	public void resourceAvailable(BuildResource r) {
		availableResources.put(r.compareAs(), r);
		dependencies.ensure(r);
		
		for (Notification n : notifications)
			n.dispatch(r);
	}

	public void registerNature(Class<?> cls) {
		try
		{
			natures.put(cls, cls.getConstructor(BuildContext.class).newInstance(this));
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getNature(Class<T> cls) {
		return (T) natures.get(cls);
	}

	public void tellMeAbout(Nature nature, Class<? extends BuildResource> cls) {
		notifications.add(new Notification(cls, nature));
	}

	public boolean addDependency(Strategem dependent, BuildResource resource) {
		StrategemResource node = new StrategemResource(dependent);
		if (dependencies.hasLink(node, resource))
			return false;
		System.out.println("Added dependency from " + dependent + " on " + resource);
		dependencies.ensureLink(node, resource);
		if (resource.getBuiltBy() != null)
			moveUp(dependent, resource.getBuiltBy());
		return true;
	}

	public Iterable<BuildResource> getDependencies(Strategem dependent) {
		StrategemResource node = new StrategemResource(dependent);
		return dependencies.allChildren(node);
	}

	public BuildResource getPendingResource(PendingResource pending) {
		BuildResource ret = getPendingResourceIfAvailable(pending);
		if (ret != null)
			return ret;
		
		String resourceName = pending.compareAs();
		System.out.println("Resource " + resourceName + " not found.  Available Resources are:");
		for (String s : availableResources.keySet())
			System.out.println("  " + s);

		throw new UtilException("There is no resource called " + resourceName);
	}

	private boolean isResourceAvailable(BuildResource br)
	{
		if (br instanceof PendingResource)
			return getPendingResourceIfAvailable((PendingResource) br) != null;
		return availableResources.containsKey(br.compareAs());
	}
	
	private BuildResource getPendingResourceIfAvailable(PendingResource pending) {
		String resourceName = pending.compareAs();
		if (availableResources.containsKey(resourceName))
			return availableResources.get(resourceName);
		
		// This time I'm not going to worry about uniqueness
		Pattern p = Pattern.compile(".*" + resourceName.toLowerCase()+".*");
		for (BuildResource br : availableResources.values())
			if (p.matcher(br.compareAs().toLowerCase()).matches())
				return br;
		
		return null;
	}

	public void figureDirtyness(StrategemResource node, boolean buildAll) {
		Strategem s = node.getBuiltBy();
		OrderedFileList files = s.sourceFiles();
		boolean isDirty;
		if (files == null)
		{
			System.out.println("   **** NULL FILE LIST IN " + node +  " ***");
			isDirty = true;
		}
		else
			isDirty = GitHelper.checkFiles(node.isClean() && !buildAll, files, getGitCacheFile(node));
		if (isDirty || buildAll)
		{
			if (buildAll)
				System.out.println("Marking " + node + " dirty due to --build-all");
			else
				System.out.println("Marking " + node + " dirty due to git hash-object");
			node.markDirty();
			for (StrategemResource d : figureDependentsOf(node))
			{
				System.out.println("  Marking " + d + " dirty as a dependent");
				d.markDirty();
			}
		}
	}

	private File getGitCacheFile(StrategemResource node) {
		return new File(conf.getCacheDir(), FileUtils.clean(node.compareAs()));
	}

	private Iterable<StrategemResource> figureDependentsOf(BuildResource node) {
		Set<StrategemResource> ret = new HashSet<StrategemResource>();
		figureDependentsOf(ret, node);
		for (int i=strategemToExecute+1;i<strats.size();i++)
			if (strats.get(i).getBuiltBy().onCascade())
				ret.add(strats.get(i));
		return ret;
	}

	private void figureDependentsOf(Set<StrategemResource> ret,	BuildResource node) {
		Node<BuildResource> find = dependencies.find(node);
		for (Link<BuildResource> l : find.linksTo())
		{
			Node<BuildResource> n = l.getFromNode();
			BuildResource entry = n.getEntry();
			if (entry instanceof StrategemResource && !ret.contains(entry))
			{
				ret.add((StrategemResource) entry);
			}
			figureDependentsOf(ret, entry);
		}
	}

	public Config getConfig() {
		return conf;
	}

	public void buildAll() {
		buildAll = true;
	}

}
