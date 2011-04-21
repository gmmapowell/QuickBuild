package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.http.ProxyInfo;
import com.gmmapowell.http.ProxyableConnection;
import com.gmmapowell.quickbuild.build.JarResource;
import com.gmmapowell.quickbuild.build.MavenResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private final List<Strategem> strategems = new ArrayList<Strategem>();
	private final List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private final List<Tactic> buildcmds = new ArrayList<Tactic>();
	private final List<String> mvnrepos = new ArrayList<String>();
	private final ProxyInfo proxyInfo = new ProxyInfo();
	private final List<ConfigApplyCommand> applicators = new ArrayList<ConfigApplyCommand>();
	private final File qbdir;
	private final List<JarResource> availableJars = new ArrayList<JarResource>();

	private String output;
	private File mvnCache;
	private ListMap<String, BuildResource> duplicates = new ListMap<String, BuildResource>();
	private List<BuildResource> willbuild = new ArrayList<BuildResource>();
	private Map<String, File> fileProps = new HashMap<String, File>();
	private Map<String, String> varProps = new HashMap<String, String>();
	private AndroidContext acxt;

	@SuppressWarnings("unchecked")
	public Config(File qbdir)
	{
		super(ConfigApplyCommand.class, ConfigBuildCommand.class);
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

			buildcmds.addAll(s.tactics());
			
			// TODO: provide all initial resources
		}
	}
	
	public List<Tactic> getBuildCommandsInOrder() {
		return buildcmds;
	}

	public void willBuild(BuildResource br) {
		willbuild.add(br);
	}
	
	public void requireMaven(String pkginfo) {
		File mavenToFile = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenToFile.getPath());
		if (!cacheFile.exists())
			downloadFromMaven(pkginfo, mavenToFile, cacheFile);
		availableJars.add(new MavenResource(pkginfo, cacheFile));
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
	
	public void provideBaseJars(DependencyGraph<BuildResource> dependencies) {
		for (JarResource jr : availableJars)
		{
			dependencies.newNode(jr);
		}
	}

	/** Copy across all the packages which are defined in global things to a build context
	 * @param availablePackages the map to copy into
	 */
	public void supplyPackages(Map<String, JarResource> availablePackages) {
		for (JarResource jr : availableJars)
		{
			jarSupplies(jr, availablePackages);
		}
		showDuplicates();
	}

	public void jarSupplies(JarResource jarfile, Map<String, JarResource> availablePackages) {
		GPJarFile jar = new GPJarFile(jarfile.getFile());
		for (GPJarEntry e : jar)
		{
			if (!e.isClassFile())
				continue;
			String pkg = e.getPackage();
			if (!availablePackages.containsKey(pkg))
			{
				availablePackages.put(pkg, jarfile);
			}
			else if (availablePackages.get(pkg).equals(jarfile))
				continue;
			else
			{
				if (!duplicates.contains(pkg))
					duplicates.add(pkg, availablePackages.get(pkg));
				duplicates.add(pkg, jarfile);
			}
		}
		
	}
	
	public void showDuplicates() {
		for (String s : duplicates)
		{
			System.out.println("Duplicate/overlapping definitions found for package: " + s);
			for (BuildResource f : duplicates.get(s))
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
		return new File(qbdir, "cache");
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

	public BuildResource getResourceByName(String fromProjectName) {
		// TODO Auto-generated method stub
		return null;
	}

}
