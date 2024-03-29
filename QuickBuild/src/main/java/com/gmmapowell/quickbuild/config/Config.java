package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.build.android.AndroidContext;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ProxyInfo;
import com.gmmapowell.utils.ProxyableConnection;
import com.gmmapowell.vc.VCHelper;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private final List<Strategem> strategems = new ArrayList<Strategem>();
	private final List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private final ProxyInfo proxyInfo = new ProxyInfo();
	private final List<ConfigApplyCommand> applicators = new ArrayList<ConfigApplyCommand>();
	private final File qbdir;

	private String output;
	private List<BuildResource> willbuild = new ArrayList<BuildResource>();
	private Map<String, File> fileProps = new HashMap<String, File>();
	private Map<String, String> varProps = new HashMap<String, String>();
	private AndroidContext acxt;
	private final String quickBuildName;
	private final Set<BuildResource> availableResources = new HashSet<BuildResource>();
	private final ConfigFactory factory;
	private File cacheDir;
	private final ListMap<String, String> libraryContexts = new ListMap<String, String>();
	private boolean topLevel;
	public final BuildOutput outlog;
	public VCHelper helper;

	@SuppressWarnings("unchecked")
	public Config(ConfigFactory factory, BuildOutput outlog, File qbdir, String quickBuildName, String cacheDir)
	{
		super(ConfigApplyCommand.class, ConfigBuildCommand.class);
		this.factory = factory;
		this.helper = factory.vchelper;
		this.outlog = outlog;
		this.quickBuildName = quickBuildName;
		try
		{
			if (qbdir == null)
				this.qbdir = null;
			else
			{
				this.qbdir = qbdir.getCanonicalFile();
				FileUtils.chdirAbs(this.qbdir.getParentFile());
			}
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
		if (cacheDir != null)
			this.cacheDir = new File(cacheDir);
		else
			this.cacheDir = new File(getWorkingDir(), "cache");
		FileUtils.assertDirectory(this.cacheDir);
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
	
	public void done() {
		topLevel = true;
		for (ConfigApplyCommand cmd : applicators)
			cmd.applyTo(this);
		topLevel = false;

		if (output == null)
			setOutputDir("qbout");

		for (ConfigBuildCommand c : commands)
		{
			Strategem s = c.applyConfig(this);
			if (s == null)
				throw new UtilException("Applying command " + c + " did not produce a strategem");
			strategems.add(s);
		}
	}
	
	public void willBuild(BuildResource br) {
		willbuild.add(br);
	}
	
	public void tellMeAboutInitialResources(ResourceListener lsnr) {
		for (BuildResource r : availableResources)
			lsnr.resourceAvailable(r);
	}
	
	public void resourceAvailable(BuildResource br)
	{
		availableResources.add(br);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  root dir = " + FileUtils.getCurrentDir() + "\n");
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
		sb.append("Natures:\n");
		for (String s : factory.availableNatures())
		{
			Class<? extends Nature> cls = factory.natureClass(s);
			sb.append("  " + s+" (" + cls + "):\n");
			if (!factory.usesNature(cls))
				sb.append("    not referenced\n");
			else
				factory.getNature(this, cls).info(sb);
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
		return cacheDir;
	}

	public File getLogDir() {
		return new File(getWorkingDir(), "logs");
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

	public boolean hasPath(String name) {
		return fileProps.containsKey(name);
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

	public String getVarIfDefined(String name, String otherwise) {
		if (!varProps.containsKey(name))
			return otherwise;
		return varProps.get(name);
	}

	public List<Strategem> getStrategems() {
		return strategems;
	}

	public ProxyableConnection newConnection(String urlPath) {
		return proxyInfo.newConnection(urlPath);
	}

	public <T extends Nature> T getNature(Class<T> cls) {
		return factory.getNature(this, cls);
	}

	public boolean hasVar(String option) {
		return varProps.containsKey(option);
	}

	public void bindLibraryContext(String context, String library) {
		libraryContexts.add(library, context);
	}

	public boolean matchesContext(BuildResource jr, String context) {
		String askedFor = jr.compareAs();
//		System.out.println("Asked for " + jr + " in context " + context);
//		System.out.println(libraryContexts);
		
		// Because of matching, the resource "jr" could match a number of context entries.
		// We want to allow it to try to match as many as possible looking for a hit,
		// but if it matches one or more and doesn't hit any of them, "false" should be the answer.
		// However, if it simply doesn't match anything, "true" is the answer ...
		boolean ret = true;
		for (String s : libraryContexts)
		{
			if (askedFor.contains(s))
			{
				for (String c : libraryContexts.get(s))
					if (c.equals(context))
						return true;
				ret = false;
			}
		}
//		if (!ret)
//			System.out.println("  ... rejecting request");
		return ret;
	}

	public boolean isTopLevel() {
		return topLevel;
	}
}
