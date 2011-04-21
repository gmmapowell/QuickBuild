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
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.utils.DateUtils;

public class BuildContext {
	private final Map<String, JarResource> availablePackages = new HashMap<String, JarResource>();
//	private final ListMap<String, Project> projectPackages = new ListMap<String, Project>();
//	private final ListMap<Project, File> previouslyBuilt = new ListMap<Project, File>();
//	private final StateMap<Project, SetMap<String, File>> packagesProvidedByDirectoriesInProject = new StateMap<Project, SetMap<String, File>>();
	private final List<BuildResource> builtResources = new ArrayList<BuildResource>();
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

	public BuildContext(Config conf) {
		this.conf = conf;
		conf.supplyPackages(availablePackages);
		conf.provideBaseJars(dependencies);
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
		strats = conf.getStrategems();
		for (Strategem s : strats)
		{
			// TODO: understand how this should work for these different cases
			for (BuildResource br : s.needsResources())
			{
				dependencies.ensure(br);
			}			

			for (BuildResource br : s.providesResources())
			{
				// conf.willBuild(br);
				dependencies.ensure(br);
			}
			
			for (BuildResource br : s.buildsResources())
			{
				conf.willBuild(br);
				dependencies.ensure(br);
			}
		}
	}
	
	/* TODO: java nature
	public void addClassDirForProject(Project proj, File dir)
	{
		previouslyBuilt.add(proj, dir);
		SetMap<String, File> dirProvider = packagesProvidedByDirectoriesInProject.require(proj, SetMap.class);
		for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			dirProvider.add(FileUtils.convertToDottedName(f.getParentFile()), dir);
	}
	*/

	public void addBuiltJar(JarResource jar) {
		addBuiltResource(jar);
		conf.jarSupplies(jar, availablePackages);
		conf.showDuplicates();
	}

	public void addBuiltResource(BuildResource resource) {
		/* TODO: dependencies
		System.out.println("The resource '" + resource + "' has been provided");
		builtResources.add(resource);
		dependencies.ensure(resource);
		if (resource.getBuiltBy() != null)
			dependencies.ensureLink(resource, resource.getBuiltBy());
			*/
	}

	// TODO: this should be in Java Nature
	public void addAllProjectDirs(RunClassPath classpath) {
		/*
		if (packagesProvidedByDirectoriesInProject.containsKey(project))
		{
			SetMap<String, File> setMap = packagesProvidedByDirectoriesInProject.get(project);
			for (File f : setMap.values())
				classpath.add(f);
		}
		*/			
	}
	
	// TODO: this is more general than just a java build command, but what?
	public void addDependency(JavaBuildCommand javaBuildCommand, String needsJavaPackage) {
		/*
		// First, try and resolve it with a base jar, or a built jar
		if (availablePackages.containsKey(needsJavaPackage))
		{
			JarResource provider = availablePackages.get(needsJavaPackage);
			javaBuildCommand.addToClasspath(provider.getFile());
			// TODO: this is grouping all commands to the same project, which is losing some info.  I think Eclipse does the same though.
			dependencies.ensureLink(javaBuildCommand.getProject(), provider);
			if (provider.getBuiltBy() != null)
				dependencies.ensureLink(javaBuildCommand.getProject(), provider.getBuiltBy());
			return; // do something
		}
		
		// Then see if it is somewhere else in the same project
		if (packagesProvidedByDirectoriesInProject.containsKey(javaBuildCommand.getProject()))
		{
			SetMap<String, File> listMap = packagesProvidedByDirectoriesInProject.get(javaBuildCommand.getProject());
			if (listMap.contains(needsJavaPackage))
			{
				for (File f : packagesProvidedByDirectoriesInProject.get(javaBuildCommand.getProject()).get(needsJavaPackage))
				{
					javaBuildCommand.addToClasspath(f);
				}
				return;
			}
		}
		
		// OK, try and move the projects around a bit
		if (projectPackages.contains(needsJavaPackage))
		{
			List<Project> list = projectPackages.get(needsJavaPackage);
			for (Project p : list)
			{
				moveUp(javaBuildCommand, p);
				dependencies.ensureLink(javaBuildCommand.getProject(), p);
			}
			return;
		}
		throw new JavaBuildFailure("cannot find any code that defines package " + needsJavaPackage);
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
		System.out.println((commandToExecute+1) + ": " + bc);
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
			if (currentCommands != null && currentCommands.hasNext())
				return currentCommands.next();
			
			if (commandToExecute >= strats.size())
				return null;
	
			currentStrat = strats.get(commandToExecute);
			currentCommands = currentStrat.tactics().iterator();
			
			commandToExecute++; 
		}
	}

	public void advance() {
		targetFailures = 0;
		commandToExecute++;
	}

	public void buildFail(BuildStatus outcome) {
		totalErrors++;
		if (outcome.isBroken())
			buildBroken = true;
	}

	public void tryAgain() {
		currentCommands = null;
		if (++targetFailures >= 3)
			throw new UtilException("The strategy " + currentStrat + " failed 3 times in a row");
	}

	public File getPath(String name) {
		return conf.getPath(name);
	}

	public void clearCache() {
		dependencies.clear();
	}

}
