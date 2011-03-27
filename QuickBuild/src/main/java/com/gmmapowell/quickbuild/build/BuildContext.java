package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.collections.StateMap;
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
	private final ListMap<Project, File> previouslyBuilt = new ListMap<Project, File>();
	private final StateMap<Project, SetMap<String, File>> packagesProvidedByDirectoriesInProject = new StateMap<Project, SetMap<String, File>>();
	private final List<JarResource> builtJars = new ArrayList<JarResource>();
	private final Config conf;
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();

	public BuildContext(Config conf) {
		this.conf = conf;
		conf.supplyPackages(availablePackages);
		conf.provideDependencies(dependencies);
	}

	public void addClassDirForProject(Project proj, File dir)
	{
		previouslyBuilt.add(proj, dir);
		SetMap<String, File> dirProvider = packagesProvidedByDirectoriesInProject.require(proj, SetMap.class);
		for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			dirProvider.add(FileUtils.convertToPackageName(f.getParentFile()), dir);
	}

	public void addBuiltJar(Project project, File jarfile) {
		JarResource jar = new JarResource(jarfile);
		builtJars.add(jar);
		dependencies.newNode(jar);
		dependencies.ensureLink(jar, project);
		conf.jarSupplies(jar, availablePackages);
		conf.showDuplicates();
	}
	
	// TODO: this is more general than just a java build command, but what?
	public void addDependency(JavaBuildCommand javaBuildCommand, String needsJavaPackage) {
		if (availablePackages.containsKey(needsJavaPackage))
		{
			JarResource provider = availablePackages.get(needsJavaPackage);
			javaBuildCommand.addJar(provider.getFile());
			dependencies.ensureLink(javaBuildCommand.getProject(), provider);
			return; // do something
		}
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
		throw new QuickBuildException("There is no java package " + needsJavaPackage);
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
		output.write(new File(conf.getCacheDir(), "dependencies.xml"));
	}

	public boolean execute(BuildCommand bc) {
		dependencies.ensure(bc.getProject());
		return bc.execute(this);
	}

}
