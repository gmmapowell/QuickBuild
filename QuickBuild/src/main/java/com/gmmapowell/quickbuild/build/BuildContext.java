package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.collections.StateMap;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class BuildContext {
	private Map<String, File> availablePackages = new HashMap<String, File>();
	private ListMap<Project, File> previouslyBuilt = new ListMap<Project, File>();
	private StateMap<Project, SetMap<String, File>> packagesProvidedByDirectoriesInProject = new StateMap<Project, SetMap<String, File>>();

	public BuildContext(Config conf) {
		conf.supplyPackages(availablePackages);
	}

	public void addClassDirForProject(Project proj, File dir)
	{
		previouslyBuilt.add(proj, dir);
		SetMap<String, File> dirProvider = packagesProvidedByDirectoriesInProject.require(proj, SetMap.class);
		for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			dirProvider.add(FileUtils.convertToPackageName(f.getParentFile()), dir);
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
