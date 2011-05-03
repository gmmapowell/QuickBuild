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
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.DateUtils;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;

public class BuildContext implements ResourceListener {
	private static class ComparisonResource extends SolidResource {
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

	private final Config conf;
	private final Map<String, BuildResource> availableResources = new TreeMap<String, BuildResource>();
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final List<JUnitFailure> failures = new ArrayList<JUnitFailure>();
	private final File dependencyFile;
	private final File buildOrderFile;
	private int commandToExecute = -1;
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
	private final boolean buildAll;
	private final List<Pattern> showArgsFor = new ArrayList<Pattern>();
	private final List<Pattern> showDebugFor = new ArrayList<Pattern>();

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
		for (StrategemResource node : strats)
		{
			Strategem s = node.getBuiltBy();
//			System.out.println("Configuring " + s);
			dependencies.ensure(node);
			
			// TODO: understand how this should work for these different cases
			for (BuildResource br : s.needsResources())
			{
				dependencies.ensure(br);
			}			

			for (BuildResource br : s.providesResources())
			{
				// conf.willBuild(br);
				dependencies.ensure(br);
				resourceAvailable(br);
			}
			
			for (BuildResource br : s.buildsResources())
			{
				conf.willBuild(br);
				dependencies.ensure(br);
			}
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
		for (int idx=commandToExecute+1;idx<strats.size();idx++)
		{
			if (strats.get(idx).getBuiltBy() == required)
			{
				StrategemResource sr = strats.remove(idx);
				strats.add(commandToExecute, sr);
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
			return;
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
			throw new QuickBuildCacheException("Did not find any build commands for " + from);
		}
	}

	public void loadDependencyCache()
	{
		if (!dependencyFile.canRead())
			return;
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
		// figure out if we need to build this
		boolean doit = true;
		if (currentStrat.isClean())
		{
			doit = false;
			System.out.print("  ");
		}
		else
			System.out.print("* ");
		System.out.println((commandToExecute+1)+"."+currentStrategemCommandNo + ": " + bc);
		if (!doit)
			return BuildStatus.IGNORED;

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
			getGitCacheFile(currentStrat).delete();
		return ret;
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
				commandToExecute++; 
			}

			if (commandToExecute >= strats.size())
				return null;
	
			currentStrat = strats.get(commandToExecute);
			figureDirtyness(currentStrat, buildAll);
			currentCommands = currentStrat.getBuiltBy().tactics().iterator();
			currentStrategemCommandNo = 0;
		}
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
		if (++targetFailures >= 3)
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

	public void registerNature(Class<?> class1) {
		try
		{
			natures.put(class1, class1.getConstructor(BuildContext.class).newInstance(this));
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
		String resourceName = pending.compareAs();
		if (!availableResources.containsKey(resourceName))
		{
			System.out.println("Resource " + resourceName + " not found.  Available Resources are:");
			for (String s : availableResources.keySet())
				System.out.println("  " + s);
			throw new UtilException("There is no resource called " + resourceName);
		}
		return availableResources.get(resourceName);
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
		for (int i=commandToExecute+1;i<strats.size();i++)
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

}
