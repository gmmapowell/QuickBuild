package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class BuildContext {
	public class Background {
		private final RunProcess proc;
		private final CompleteBackgroundCommand cc;

		public Background(RunProcess proc, CompleteBackgroundCommand cc) {
			this.proc = proc;
			this.cc = cc;
		}
	}

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
	private final boolean alwaysRunTests;

	private final File utilsJar;

	public String upTo;

	public boolean grandFallacy;

	public final BuildOutput output;

	public final boolean doubleQuick;

	public final boolean allTests;

	private List<Background> backgrounds = new ArrayList<Background>();

	private boolean showWhy;

	private boolean showTimings;

	public BuildContext(Config conf, ConfigFactory configFactory, BuildOutput output, boolean blankMemory, boolean buildAll, boolean debug, List<String> showArgsFor, List<String> showDebugFor, boolean quiet, File utilsJar, String upTo, boolean doubleQuick, boolean allTests, boolean gfMode, boolean alwaysTest, boolean showWhy, boolean showTimings) {
		this.conf = conf;
		this.output = output;
		this.blankMemory = blankMemory;
		this.quiet = quiet;
		this.utilsJar = utilsJar;
		this.upTo = upTo;
		this.doubleQuick = doubleQuick;
		this.allTests = allTests;
		this.showWhy = showWhy;
		this.grandFallacyMode = gfMode;
		this.alwaysRunTests = alwaysTest;
		this.showTimings = showTimings;
		rm = new ResourceManager(conf);
		manager = new DependencyManager(conf, rm, debug);
		buildOrder = new BuildOrder(this, manager, conf.helper, buildAll, debug, showTimings);
		ehandler = new ErrorHandler(conf.getLogDir());
		for (String s : showArgsFor)
			this.showArgsFor.add(Pattern.compile(s.toLowerCase()));
		for (String s : showDebugFor)
			this.showDebugFor.add(Pattern.compile(s.toLowerCase()));
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
	
	public void setConfigVar(String var, String value) {
		conf.setVarProperty(var, value);
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
		long start = new Date().getTime();
		if (showTimings) {
			System.out.println("configure start...");
		}
		rm.configure(tactics);
		if (showTimings) {
			long time1 = new Date().getTime();
			System.out.println("configure done: " + (time1-start));
		}
		for (Tactic t : tactics) {
			buildOrder.knowAbout(t);
		}
		if (showTimings) {
			long time2 = new Date().getTime();
			System.out.println("configured tactics: " + (time2-start));
		}
		if (!blankMemory)
			buildOrder.loadBuildOrderCache();
		if (showTimings) {
			long time3 = new Date().getTime();
			System.out.println("loaded build cache: " + (time3-start));
		}
		manager.init(tactics);
		if (showTimings) {
			long time4 = new Date().getTime();
			System.out.println("init manager: " + (time4-start));
		}
		if (blankMemory || !manager.loadDependencyCache(tactics)) {
			buildOrder.buildAll();
			manager.figureOutDependencies(tactics);
		}
		if (showTimings) {
			long time5 = new Date().getTime();
			System.out.println("loaded dependency cache: " + (time5-start));
		}
		buildOrder.figureDirtyness(manager);
		if (showTimings) {
			long time6 = new Date().getTime();
			System.out.println("figured dirtyness: " + (time6-start));
		}
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
			if (p.matcher(bc.toString().toLowerCase()).find())
				return true;
		return false;
	}

	boolean showDebug(Tactic bc) {
		for (Pattern p : showDebugFor)
			if (p.matcher(bc.toString().toLowerCase()).find())
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

	// announce that a process has been shoved into the background and
	// we will "ultimately" need to wait for it ...
	public void background(RunProcess proc, CompleteBackgroundCommand cc) {
		backgrounds.add(new Background(proc, cc));
	}
	
	public void waitForBackgroundsToComplete() {
		for (Background b : backgrounds) {
			System.out.println("Waiting for " + b.cc.getLabel());
			while (!b.proc.isFinished()) {
				try { Thread.sleep(250); } catch (Exception ex) { }
			}
			b.cc.completeCommand(this, b.proc);
		}
	}

	public boolean alwaysRunTests() {
		return alwaysRunTests;
	}

	public boolean why() {
		return showWhy;
	}
}
