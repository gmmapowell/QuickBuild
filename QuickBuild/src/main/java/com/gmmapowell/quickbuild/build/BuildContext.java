package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class BuildContext {
	private BuildOrder buildOrder;

	private DependencyManager manager;
	private final Config conf;
	
	private final ErrorHandler ehandler;
	private List<Strategem> strats = new ArrayList<Strategem>();
	private final List<Pattern> showArgsFor = new ArrayList<Pattern>();
	private final List<Pattern> showDebugFor = new ArrayList<Pattern>();
	private final ResourceManager rm;

	public BuildContext(Config conf, ConfigFactory configFactory, boolean buildAll, List<String> showArgsFor, List<String> showDebugFor) {
		this.conf = conf;
		rm = new ResourceManager(conf);
		buildOrder = new BuildOrder(this, buildAll);
		manager = new DependencyManager(conf, rm, buildOrder);
		ehandler = new ErrorHandler(conf.getLogDir());
		for (String s : showArgsFor)
			this.showArgsFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (String s : showDebugFor)
			this.showDebugFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (Nature n : configFactory.installedNatures())
			registerNature(n.getClass(), n);
		for (Strategem s : conf.getStrategems())
			strats.add(s);
	}
	
	public BuildOrder getBuildOrder() {
		return buildOrder;
	}

	public ErrorHandler getErrorHandler() {
		return ehandler;
	}

	public DependencyManager getDependencyManager() {
		return manager;
	}

	public ResourceManager getResourceManager() {
		return rm;
	}

	public void configure()
	{
		rm.configure(strats);
		try
		{
			buildOrder.loadBuildOrderCache();
			manager.loadDependencyCache();
			buildOrder.attachStrats(strats);
			manager.attachStrats(strats);
		}
		catch (QuickBuildCacheException ex) {
			// the cache failed to load because of inconsistencies or whatever
			// ignore it and try again
			System.out.println("Cache was out of date; ignoring");
			System.out.println("  " + ex.getMessage());
			if (ex.getCause() != null)
				System.out.println("  > "+ ex.getCause().getMessage());
			manager.figureOutDependencies(strats);
			buildOrder.buildAll();
		}

		buildOrder.figureDirtyness(manager);
	}

	public File getPath(String name) {
		return conf.getPath(name);
	}

	public File getCacheFile(String file) {
		return new File(conf.getCacheDir(), file);
	}
	
	public File getGitCacheFile(ExecuteStrategem node, String ext) {
		return new File(conf.getCacheDir(), FileUtils.clean(node.name()) + ext);
	}

	public void tellMeAbout(Nature nature, Class<? extends BuildResource> cls) {
		rm.tellMeAbout(nature, cls);
	}

	boolean showArgs(Tactic bc) {
		for (Pattern p : showArgsFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	boolean showDebug(Tactic bc) {
		for (Pattern p : showDebugFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	public void registerNature(Class<?> cls, Nature n) {
		if (n instanceof BuildContextAware)
			((BuildContextAware)n).provideBuildContext(this);
	}

	public <T extends Nature> T getNature(Class<T> cls) {
		return conf.getNature(cls);
	}

	public ErrorCase failure(List<String> args, String stdout, String stderr) {
		return ehandler.failure(args, stdout, stderr);
	}
	
	public boolean addDependency(Strategem dependent, BuildResource resource) {
		if (dependent == null)
			throw new QuickBuildException("The strategem cannot be null");
		if (resource == null)
			throw new QuickBuildException("The resource cannot be null");
		return manager.addDependency(dependent, resource);
	}
	
	public Iterable<BuildResource> getDependencies(Strategem parent) {
		return manager.getDependencies(parent);
	}

	public <T extends BuildResource> Iterable<BuildResource> getResources(Class<T> cls) {
		return rm.getResources(cls);
	}

	public <T extends BuildResource> T getBuiltResource(Strategem p, Class<T> ofCls) {
		return rm.getBuiltResource(p, ofCls);
	}

	public String printableDependencyGraph() {
		return manager.printableDependencyGraph();
	}

	public String printableBuildOrder() {
		return buildOrder.printOut();
	}

	// People shouldn't be doing this for themselves
	@Deprecated
	public BuildResource getPendingResource(PendingResource s) {
		return rm.getPendingResource(s);
	}

	// Use the resource manager directly
	@Deprecated
	public void resourceAvailable(BuildResource r) {
		rm.resourceAvailable(r);
	}
}
