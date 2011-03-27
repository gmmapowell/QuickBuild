package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ProxyInfo;
import com.gmmapowell.http.ProxyableConnection;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private String output;
	private List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private List<BuildCommand> buildcmds = new ArrayList<BuildCommand>();
	private List<String> mvnrepos = new ArrayList<String>();
	private File mvnCache;
	private final ProxyInfo proxyInfo = new ProxyInfo();
	private List<ConfigApplyCommand> applicators = new ArrayList<ConfigApplyCommand>();
	private final File qbdir;

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
			buildcmds.addAll(c.buildCommands(this));
		}
	}
	
	public List<BuildCommand> getBuildCommandsInOrder() {
		return buildcmds;
	}
	
	public void requireMaven(String pkginfo) {
		File mavenToFile = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenToFile.getPath());
		if (cacheFile.exists())
			return;
		
		if (mvnrepos.size() == 0)
			throw new QuickBuildException("There are no maven repositories specified");
		for (String repo : mvnrepos)
		{
			ProxyableConnection conn = proxyInfo.newConnection(FileUtils.urlPath(repo, mavenToFile));
			try {
				FileUtils.assertDirectory(cacheFile.getParentFile());
				FileOutputStream fos = new FileOutputStream(cacheFile);
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
	public void supplyPackages(Map<String, String> availablePackages) {
		// TODO Auto-generated method stub
		
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
