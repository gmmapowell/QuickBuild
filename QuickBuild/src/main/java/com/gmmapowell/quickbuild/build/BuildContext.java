package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.graphs.Link;
import com.gmmapowell.graphs.Node;
import com.gmmapowell.graphs.NodeWalker;
import com.gmmapowell.quickbuild.build.java.JUnitFailure;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.DateUtils;
import com.gmmapowell.utils.FileUtils;
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
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final List<JUnitFailure> failures = new ArrayList<JUnitFailure>();
	private final File dependencyFile;
	private final File buildOrderFile;
	private Set<Tactic> needed = null; // is null to indicate build all
	private int commandToExecute = -1;
	private int targetFailures;
	private Date buildStarted;
	private int totalErrors;
	private boolean buildBroken;
	private int projectsWithTestFailures;
	public List<Strategem> strats;
	private Strategem currentStrat;
	private Iterator<? extends Tactic> currentCommands;
	private int currentStrategemCommandNo;
	private boolean moveOn = true;
	private Map<Class<?>, Object> natures = new HashMap<Class<?>, Object>();
	private final List<Notification> notifications = new ArrayList<BuildContext.Notification>();
	private Tactic repeat;

	public BuildContext(Config conf) {
		this.conf = conf;
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
		buildOrderFile = new File(conf.getCacheDir(), "buildOrder.xml");
		strats = conf.getStrategems();
	}
	
	public void configure()
	{
		conf.tellMeAboutExtantResources(this);
		for (Strategem s : strats)
		{
			StrategemResource node = new StrategemResource(s);
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
	
	public void addBuiltResource(BuildResource resource) {
		resourceAvailable(resource);
		/* TODO: dependencies
		System.out.println("The resource '" + resource + "' has been provided");
		builtResources.add(resource);
		dependencies.ensure(resource);
		if (resource.getBuiltBy() != null)
			dependencies.ensureLink(resource, resource.getBuiltBy());
			*/
	}

	// TODO: should reference strategems, not build commands
	// but the build commands should go to
	private void moveUp(Strategem current, Strategem required) {
		for (int idx=commandToExecute+1;idx<strats.size();idx++)
		{
			if (strats.get(idx) == required)
			{
				strats.remove(idx);
				strats.add(commandToExecute, required);
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
				Strategem s = strats.get(i);
				if (new StrategemResource(s).compareAs().equals(from))
				{
					if (i != moveTo)
					{
						strats.remove(i);
						strats.add(moveTo, s);
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
					System.out.println(target + " <= " + source);
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
		for (Strategem s : strats)
		{
			XMLElement item = output.addElement("BuildItem");
			item.setAttribute("strategem", new StrategemResource(s).compareAs());
		}
		FileUtils.assertDirectory(buildOrderFile.getParentFile());
		output.write(buildOrderFile);
	}

	public BuildStatus execute(Tactic bc) {
		// Record when first build started
		if (buildStarted == null)
			buildStarted = new Date();
		
		// figure out if we need to build this
		boolean doit = true;
		if (needed != null && !needed.contains(bc))
		{
			doit = false;
			System.out.print("  ");
		}
		else
			System.out.print("* ");
		System.out.println((commandToExecute+1)+"."+currentStrategemCommandNo + ": " + bc);
		if (!doit)
			return BuildStatus.IGNORED;
		return bc.execute(this);
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
		else
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
				commandToExecute++; 

			if (commandToExecute >= strats.size())
				return null;
	
			currentStrat = strats.get(commandToExecute);
			currentCommands = currentStrat.tactics().iterator();
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

	public void addDependency(Strategem dependent, BuildResource resource) {
		StrategemResource node = new StrategemResource(dependent);
		dependencies.ensureLink(node, resource);
		if (resource.getBuiltBy() != null)
			moveUp(dependent, resource.getBuiltBy());
	}

	public Iterable<BuildResource> getDependencies(Strategem dependent) {
		StrategemResource node = new StrategemResource(dependent);
		return dependencies.allChildren(node);
	}

}
