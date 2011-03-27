package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.collections.StateMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.graphs.Link;
import com.gmmapowell.graphs.Node;
import com.gmmapowell.graphs.NodeWalker;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;

public class BuildContext {
	private final Map<String, JarResource> availablePackages = new HashMap<String, JarResource>();
	private final ListMap<String, Project> projectPackages = new ListMap<String, Project>();
	private final ListMap<Project, File> previouslyBuilt = new ListMap<Project, File>();
	private final StateMap<Project, SetMap<String, File>> packagesProvidedByDirectoriesInProject = new StateMap<Project, SetMap<String, File>>();
	private final List<JarResource> builtJars = new ArrayList<JarResource>();
	private final Config conf;
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final List<JUnitFailure> failures = new ArrayList<JUnitFailure>();
	private final File dependencyFile;
	private final List<BuildCommand> cmds;
	private int commandToExecute;
	private int targetFailures;

	public BuildContext(Config conf) {
		this.conf = conf;
		conf.supplyPackages(availablePackages);
		conf.provideBaseJars(dependencies);
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
		cmds = conf.getBuildCommandsInOrder();
		for (BuildCommand cmd : cmds)
		{
			Set<String> packagesForCommand = cmd.getPackagesProvided();
			if (packagesForCommand != null)
			{
				for (String s : packagesForCommand)
					projectPackages.add(s, cmd.getProject());
			}
		}
	}

	public void addClassDirForProject(Project proj, File dir)
	{
		previouslyBuilt.add(proj, dir);
		SetMap<String, File> dirProvider = packagesProvidedByDirectoriesInProject.require(proj, SetMap.class);
		for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			dirProvider.add(FileUtils.convertToDottedName(f.getParentFile()), dir);
	}

	public void addBuiltJar(Project project, File jarfile) {
		JarResource jar = new JarResource(jarfile);
		builtJars.add(jar);
		dependencies.newNode(jar);
		dependencies.ensureLink(jar, project);
		conf.jarSupplies(jar, availablePackages);
		conf.showDuplicates();
	}
	
	public void addAllProjectDirs(RunClassPath classpath, Project project) {
		if (packagesProvidedByDirectoriesInProject.containsKey(project))
		{
			SetMap<String, File> setMap = packagesProvidedByDirectoriesInProject.get(project);
			for (File f : setMap.values())
				classpath.add(f);
		}			
	}
	
	// TODO: this is more general than just a java build command, but what?
	public void addDependency(JavaBuildCommand javaBuildCommand, String needsJavaPackage) {
		// First, try and resolve it with a base jar, or a built jar
		if (availablePackages.containsKey(needsJavaPackage))
		{
			JarResource provider = availablePackages.get(needsJavaPackage);
			javaBuildCommand.addJar(provider.getFile());
			dependencies.ensureLink(javaBuildCommand.getProject(), provider);
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
					javaBuildCommand.addJar(f);
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
			}
			return;
		}
		throw new QuickBuildException("There is no java package " + needsJavaPackage);
	}

	private void moveUp(JavaBuildCommand from, Project p) {
		int moveTo = -1;
		for (int cmd=commandToExecute;cmd<cmds.size();cmd++)
		{
			BuildCommand bc = cmds.get(cmd);
			if (bc == from)
				moveTo = cmd;
			if (moveTo == -1)
				continue;
			if (bc.getProject() == p)
			{
				cmds.remove(cmd);
				cmds.add(moveTo++, bc);
			}
		}
	}

	public void loadCache() {
	}

	public void saveDependencies() {
		final XML output = XML.create("1.0", "Dependencies");
		dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {
			@Override
			public void present(Node<BuildResource> node) {
				if (!(node.getEntry() instanceof Project))
					return;
				XMLElement dep = output.addElement("Dependency");
				dep.setAttribute("from", node.getEntry().toString());
				for (Link<BuildResource> l : node.linksFrom())
				{
					XMLElement ref = dep.addElement("References");
					ref.setAttribute("on", l.getTo().toString());
				}
			}
		});
		output.write(dependencyFile);
	}

	public boolean execute(BuildCommand bc) {
		dependencies.ensure(bc.getProject());
		return bc.execute(this);
	}

	public void junitFailure(JUnitRunCommand cmd, String stdout, String stderr) {
		JUnitFailure failure = new JUnitFailure(cmd, stdout, stderr);
		failures.add(failure);
	}

	public void showAnyErrors() {
		for (JUnitFailure failure : failures)
			failure.show();
	}

	public BuildCommand next() {
		if (commandToExecute >= cmds.size())
			return null;
		
		BuildCommand bc = cmds.get(commandToExecute);
		System.out.println((commandToExecute+1) + ": " + bc);
		return bc;
	}

	public void advance() {
		targetFailures = 0;
		commandToExecute++;
	}

	public void buildFail() {
		BuildCommand bc = cmds.get(commandToExecute);
		if (++targetFailures >= 3)
			throw new UtilException("The command " + bc + " failed 3 times in a row");
	}

}
