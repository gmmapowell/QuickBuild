package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class BuildContext {
	private BuildOrder buildOrder;

	private DependencyManager manager;
	private final Config conf;
	
	private final ErrorHandler ehandler;
	private final List<Tactic> tactics = new ArrayList<Tactic>();
	private final List<Pattern> showArgsFor = new ArrayList<Pattern>();
	private final List<Pattern> showDebugFor = new ArrayList<Pattern>();
	private final ResourceManager rm;

	private final boolean blankMemory;

	private final boolean quiet;
	private final boolean grandFallacyMode;

	private final File utilsJar;

	public String upTo;

	public boolean grandFallacy;

	public final BuildOutput output;

	public final boolean doubleQuick;

	public final boolean allTests;


	public BuildContext(Config conf, ConfigFactory configFactory, BuildOutput output, boolean blankMemory, boolean buildAll, boolean debug, List<String> showArgsFor, List<String> showDebugFor, boolean quiet, File utilsJar, String upTo, boolean doubleQuick, boolean allTests, boolean gfMode) {
		this.conf = conf;
		this.output = output;
		this.blankMemory = blankMemory;
		this.quiet = quiet;
		this.utilsJar = utilsJar;
		this.upTo = upTo;
		this.doubleQuick = doubleQuick;
		this.allTests = allTests;
		grandFallacyMode = gfMode;
		rm = new ResourceManager(conf);
		manager = new DependencyManager(conf, rm, debug);
		buildOrder = new BuildOrder(this, manager, buildAll, debug);
		ehandler = new ErrorHandler(conf.getLogDir());
		for (String s : showArgsFor)
			this.showArgsFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (String s : showDebugFor)
			this.showDebugFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (Nature n : configFactory.installedNatures())
			registerNature(n.getClass(), n);
		for (Strategem s : conf.getStrategems())
			tactics.addAll(s.tactics());
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

	public boolean quietMode()
	{
		return quiet;
	}
	
	public boolean grandFallacyMode() {
		return grandFallacyMode;
	}

	public File getUtilsJar() {
		return utilsJar;
	}
	
	public void configure()
	{
		rm.configure(tactics);
		for (Tactic t : tactics) {
			buildOrder.knowAbout(t);
		}
		if (!blankMemory)
			buildOrder.loadBuildOrderCache();
		manager.init(tactics);
		if (blankMemory || !manager.loadDependencyCache(tactics)) {
			buildOrder.buildAll();
			manager.figureOutDependencies(tactics);
		}
		buildOrder.figureDirtyness(manager);
	}

	public boolean hasPath(String name) {
		return conf.hasPath(name);
	}

	public File getPath(String name) {
		return conf.getPath(name);
	}

	public File getCacheFile(String file) {
		return new File(conf.getCacheDir(), file);
	}
	
	public File getGitCacheFile(String name, String ext) {
		return new File(conf.getCacheDir(), FileUtils.clean(name) + ext);
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
	
	public boolean addDependency(Tactic dependent, BuildResource resource, boolean wantDebug) {
		if (dependent == null)
			throw new QuickBuildException("The tactic cannot be null");
		if (resource == null)
			throw new QuickBuildException("The resource cannot be null");
		return manager.addDependency(dependent, resource, wantDebug);
	}
	
	public Iterable<BuildResource> getDependencies(Tactic t) {
		return manager.getDependencies(t);
	}

	public Iterable<BuildResource> getTransitiveDependencies(Tactic t) {
		return manager.getTransitiveDependencies(t);
	}
	
	public void builtResource(BuildResource r) {
		builtResource(r, true);
	}

	public void builtResource(BuildResource r, boolean analyze) {
		if (r != null)
			rm.resourceAvailable(r, analyze);
	}
	
	public <T extends BuildResource> Iterable<BuildResource> getResources(Class<T> cls) {
		return rm.getResources(cls);
	}

	public String printableDependencyGraph() {
		return manager.printableDependencyGraph();
	}

	public String printableBuildOrder(boolean b) {
		return buildOrder.printOut(b);
	}
}
