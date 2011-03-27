package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.collections.StateMap;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class BuildContext {
	private final Map<String, File> availablePackages = new HashMap<String, File>();
	private final ListMap<Project, File> previouslyBuilt = new ListMap<Project, File>();
	private final StateMap<Project, SetMap<String, File>> packagesProvidedByDirectoriesInProject = new StateMap<Project, SetMap<String, File>>();
	private final List<File> builtJars = new ArrayList<File>();
	private final Config conf;

	public BuildContext(Config conf) {
		this.conf = conf;
		conf.supplyPackages(availablePackages);
	}

	public void addClassDirForProject(Project proj, File dir)
	{
		previouslyBuilt.add(proj, dir);
		SetMap<String, File> dirProvider = packagesProvidedByDirectoriesInProject.require(proj, SetMap.class);
		for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			dirProvider.add(FileUtils.convertToPackageName(f.getParentFile()), dir);
	}

	public void addBuiltJar(Project project, File jarfile) {
		builtJars.add(jarfile);
		conf.jarSupplies(jarfile, availablePackages);
		conf.showDuplicates();
	}
	
	// TODO: this is more general than just a java build command, but what?
	public void addDependency(JavaBuildCommand javaBuildCommand, String needsJavaPackage) {
		if (availablePackages.containsKey(needsJavaPackage))
		{
			javaBuildCommand.addJar(availablePackages.get(needsJavaPackage));
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

}
