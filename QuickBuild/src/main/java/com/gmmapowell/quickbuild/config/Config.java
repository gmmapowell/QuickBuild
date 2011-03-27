package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ProxyInfo;
import com.gmmapowell.http.ProxyableConnection;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private final List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private final List<BuildCommand> buildcmds = new ArrayList<BuildCommand>();
	private final List<String> mvnrepos = new ArrayList<String>();
	private final ProxyInfo proxyInfo = new ProxyInfo();
	private final List<ConfigApplyCommand> applicators = new ArrayList<ConfigApplyCommand>();
	private final File qbdir;
	private final List<File> availableJars = new ArrayList<File>();

	private String output;
	private File mvnCache;
	private Map<File, Project> projects = new HashMap<File, Project>();

	@SuppressWarnings("unchecked")
	public Config(File qbdir)
	{
		super(ConfigApplyCommand.class, ConfigBuildCommand.class);
		try
		{
			this.qbdir = qbdir.getCanonicalFile();
			FileUtils.chdir(qbdir);
			mvnrepos.add("http://repo1.maven.org/maven2");
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	@Override
	public void addChild(ConfigCommand cmd) {
		if (cmd instanceof ConfigApplyCommand)
			applicators.add((ConfigApplyCommand) cmd);
		else if (cmd instanceof ConfigBuildCommand)
			commands.add((ConfigBuildCommand) cmd);
		else
			throw new QuickBuildException("'" + cmd + "' is not an acceptable child");
	}
	
	public void setOutputDir(String output) {
		if (this.output != null)
			throw new QuickBuildException("You cannot set the output dir more than once");
		this.output = output;
	}
	
	public void clearMavenRepos() {
		mvnrepos.clear();
	}
	
	public void addMavenRepo(String repo) {
		mvnrepos.add(repo);
	}

	public File getOutputDir(File forProject) {
		return new File(forProject, output);
	}
	
	public void done() {
		mvnCache = FileUtils.relativePath(qbdir, "mvncache");
		if (!mvnCache.exists())
			if (!mvnCache.mkdirs())
				throw new QuickBuildException("Cannot create directory " + mvnCache);
		if (!mvnCache.isDirectory())
			throw new QuickBuildException("Maven cache directory '" + mvnCache + "' is not a directory");

		for (ConfigApplyCommand cmd : applicators)
			cmd.applyTo(this);

		for (ConfigBuildCommand c : commands)
		{
			File projdir = c.projectDir();
			Project proj = new Project(projdir);
			if (!projects.containsKey(projdir))
				projects.put(projdir, proj);
			buildcmds.addAll(c.buildCommands(this, proj));
		}
	}
	
	public List<BuildCommand> getBuildCommandsInOrder() {
		return buildcmds;
	}
	
	public void requireMaven(String pkginfo) {
		File mavenToFile = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenToFile.getPath());
		if (!cacheFile.exists())
			downloadFromMaven(pkginfo, mavenToFile, cacheFile);
		availableJars.add(cacheFile);
	}

	private void downloadFromMaven(String pkginfo, File mavenToFile, File cacheTo) {
		if (mvnrepos.size() == 0)
			throw new QuickBuildException("There are no maven repositories specified");
		for (String repo : mvnrepos)
		{
			ProxyableConnection conn = proxyInfo.newConnection(FileUtils.urlPath(repo, mavenToFile));
			try {
				FileUtils.assertDirectory(cacheTo.getParentFile());
				FileOutputStream fos = new FileOutputStream(cacheTo);
				FileUtils.copyStream(conn.getInputStream(), fos);
				fos.close();
				System.out.println("Downloaded " + pkginfo + " from " + repo);
				return;
			} catch (IOException e) {
				System.out.println("Could not find " + pkginfo + " at " + repo + ":\n  " + e.getMessage());
			}
		}
		throw new QuickBuildException("Could not find maven package " + pkginfo);
	}

	/** Copy across all the packages which are defined in global things to a build context
	 * @param availablePackages the map to copy into
	 */
	public void supplyPackages(Map<String, File> availablePackages) {
		ListMap<String, File> duplicates = new ListMap<String, File>();
		for (File f : availableJars)
		{
			GPJarFile jar = new GPJarFile(f);
			for (GPJarEntry e : jar)
			{
				if (!e.isClassFile())
					continue;
				String pkg = e.getPackage();
				if (!availablePackages.containsKey(pkg))
				{
					availablePackages.put(pkg, f);
				}
				else if (availablePackages.get(pkg).equals(f))
					continue;
				else
				{
					if (!duplicates.contains(pkg))
						duplicates.add(pkg, availablePackages.get(pkg));
					duplicates.add(pkg, f);
				}
			}
		}
		
		for (String s : duplicates)
		{
			System.out.println("Duplicate/overlapping definitions found for package: " + s);
			for (File f : duplicates.get(s))
				System.out.println("  " + f);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  root dir = " + FileUtils.getCurrentDir() + "\n");
		sb.append("  mvncache = " + mvnCache + "\n");
		for (String s : mvnrepos)
			sb.append("    repo: " + s + "\n");
		sb.append("  output = " + output + "\n");
		sb.append("  qbdir = " + qbdir + "\n");
		sb.append("Commands:\n");
		for (ConfigCommand cc : commands)
			sb.append(cc);
		return sb.toString();
	}
}
