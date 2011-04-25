package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ProxyInfo;
import com.gmmapowell.http.ProxyableConnection;
import com.gmmapowell.quickbuild.build.android.AndroidContext;
import com.gmmapowell.quickbuild.build.java.MavenResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private final List<Strategem> strategems = new ArrayList<Strategem>();
	private final List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private final List<String> mvnrepos = new ArrayList<String>();
	private final ProxyInfo proxyInfo = new ProxyInfo();
	private final List<ConfigApplyCommand> applicators = new ArrayList<ConfigApplyCommand>();
	private final File qbdir;

	private String output;
	private File mvnCache;
	private List<BuildResource> willbuild = new ArrayList<BuildResource>();
	private Map<String, File> fileProps = new HashMap<String, File>();
	private Map<String, String> varProps = new HashMap<String, String>();
	private AndroidContext acxt;
	private final String quickBuildName;
	private final Set<BuildResource> availableResources = new HashSet<BuildResource>();

	@SuppressWarnings("unchecked")
	public Config(File qbdir, String quickBuildName)
	{
		super(ConfigApplyCommand.class, ConfigBuildCommand.class);
		this.quickBuildName = quickBuildName;
		try
		{
			if (qbdir == null)
				this.qbdir = null;
			else
			{
				this.qbdir = qbdir.getCanonicalFile();
				FileUtils.chdir(qbdir);
			}
			mvnrepos.add("http://repo1.maven.org/maven2");
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	@Override
	public void addChild(ConfigCommand cmd) {
		if (cmd instanceof DoNothingCommand)
			return;
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

	public void done() {
		mvnCache = FileUtils.relativePath(qbdir, "mvncache");
		if (!mvnCache.exists())
			if (!mvnCache.mkdirs())
				throw new QuickBuildException("Cannot create directory " + mvnCache);
		if (!mvnCache.isDirectory())
			throw new QuickBuildException("Maven cache directory '" + mvnCache + "' is not a directory");

		for (ConfigApplyCommand cmd : applicators)
			cmd.applyTo(this);

		if (output == null)
			setOutputDir("qbout");

		for (ConfigBuildCommand c : commands)
		{
			Strategem s = c.applyConfig(this);
			strategems.add(s);

			// TODO: provide all initial resources
		}
	}
	
	public void willBuild(BuildResource br) {
		willbuild.add(br);
	}
	
	public void requireMaven(String pkginfo) {
		File mavenToFile = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenToFile.getPath());
		if (!cacheFile.exists())
			downloadFromMaven(pkginfo, mavenToFile, cacheFile);
		MavenResource res = new MavenResource(pkginfo, cacheFile);
		availableResources.add(res);
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
	
	public void tellMeAboutInitialResources(ResourceListener lsnr) {
		for (BuildResource r : availableResources)
			lsnr.resourceAvailable(r);
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
		sb.append("\n");
		if (varProps.size() + fileProps.size() > 0)
		{
			sb.append("Variables:\n");
			for (Entry<String, String> kv : varProps.entrySet())
				sb.append("  V:" + kv.getKey() + " => " + kv.getValue() + "\n");
			for (Entry<String, File> kv : fileProps.entrySet())
				sb.append("  P:" + kv.getKey() + " => " + kv.getValue() + "\n");
			sb.append("\n");
		}
		sb.append("Commands:\n");
		for (ConfigCommand cc : commands)
			sb.append("  " + cc + "\n");
		return sb.toString();
	}

	public String getOutput() {
		return output;
	}

	public File getQuickBuildDir() {
		return qbdir;
	}

	public File getCacheDir() {
		return new File(getWorkingDir(), "cache");
	}

	private File getWorkingDir() {
		return new File(qbdir, quickBuildName);
	}

	public AndroidContext getAndroidContext() {
		if (acxt == null)
			acxt = new AndroidContext(this); 
		return acxt;
	}

	public void setFileProperty(String name, File path) {
		fileProps.put(name, path);
	}

	public void setVarProperty(String name, String var) {
		varProps.put(name, var);
	}

	public File getPath(String name) {
		if (!fileProps.containsKey(name))
			throw new QBConfigurationException("There is no path var " + name);
		return fileProps.get(name);
	}

	public String getVar(String name) {
		if (!varProps.containsKey(name))
			throw new QBConfigurationException("There is no var " + name);
		return varProps.get(name);
	}

	public List<Strategem> getStrategems() {
		return strategems;
	}
}
