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
import com.gmmapowell.quickbuild.build.java.JUnitFailure;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.DateUtils;

public class BuildContext implements ResourceListener {
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
	private Set<Tactic> needed = null; // is null to indicate build all
	private int commandToExecute;
	private int targetFailures;
	private Date buildStarted;
	private int totalErrors;
	private boolean buildBroken;
	private int projectsWithTestFailures;
	private List<Strategem> strats;
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
		for (int idx=commandToExecute;idx<strats.size();idx++)
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
		if (!dependencyFile.canRead())
			return;
		/* TODO: come back to me
		final XML input = XML.fromFile(dependencyFile);
		int moveTo = 0;
		List<Object[]> pass2 = new ArrayList<Object[]>();
		for (XMLElement e : input.top().elementChildren())
		{
			Project proj = null;
			String from = e.get("from");
			for (int i=moveTo;i<cmds.size();i++)
			{
				Tactic bc = cmds.get(i);
				if (bc.getProject().toString().equals(from))
				{
					proj = bc.getProject();
					if (i != moveTo)
					{
						cmds.remove(i);
						cmds.add(moveTo, bc);
					}
					moveTo++;
				}
			}
			if (proj == null)
				throw new QuickBuildCacheException("Did not find any build commands for " + from);
			pass2.add(new Object[] { proj, e.elementChildren() });
		}
		
		for (Object[] po : pass2)
		{
			Project proj = (Project) po[0];
			@SuppressWarnings("unchecked")
			List<XMLElement> elts = (List<XMLElement>) po[1];
			for (XMLElement r : elts) {
				if (r.hasTag("References"))
				{
					String on = r.get("on");
					BuildResource provider = conf.findResource(on);
					dependencies.ensureLink(proj, provider);
					if (provider instanceof JarResource)
					{
						JarResource jr = (JarResource)provider;
						for (Tactic jbc : commandsFor(proj))
						{
							if (jbc instanceof JavaBuildCommand)
								((JavaBuildCommand)jbc).addToClasspath(jr.getFile());
						}
					}
				}
				else if (r.hasTag("Provides"))
				{
					// no significance
				}
				else
					throw new QuickBuildException("The tag " + r.tag() + " is unknown");
			}
		}
		*/
	}

	public void saveDependencies() {
		System.out.println(dependencies);
		/* TODO: come back to me 
		final XML output = XML.create("1.0", "Dependencies");
		dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {
			@Override
			public void present(Node<BuildResource> node) {
				if (!(node.getEntry() instanceof Project))
					return;
				Project proj = (Project)node.getEntry();
				XMLElement dep = output.addElement("Dependency");
				dep.setAttribute("from", node.getEntry().toString());
				for (Link<BuildResource> l : node.linksFrom())
				{
					XMLElement ref = dep.addElement("References");
					BuildResource to = l.getTo();
					ref.setAttribute("on", to.toString());
				}
				for (Tactic bc : commandsFor(proj))
				{
					if (!(bc instanceof JarBuildCommand))
						continue;
					XMLElement provides = dep.addElement("Provides");
					provides.setAttribute("jar", ((JarBuildCommand)bc).getFile().getPath());
				}
			}

		});
		output.write(dependencyFile);
		*/
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
		System.out.println(commandToExecute+"."+currentStrategemCommandNo + ": " + bc);
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
			
			if (commandToExecute >= strats.size())
				return null;
	
			currentStrat = strats.get(commandToExecute);
			currentCommands = currentStrat.tactics().iterator();
			
			if (moveOn)
				commandToExecute++; 
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
