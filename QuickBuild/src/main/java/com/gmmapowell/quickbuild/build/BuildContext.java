package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.java.JUnitFailure;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.DateUtils;
import com.gmmapowell.utils.FileUtils;

/* Somewhere deep inside here, there is structure waiting to break out.
 * I think there are really 4 separate functions for this class:
 * 
 *   * Managing the build state (config, resources, etc)
 *   * Managing the build order (strats, tactics, floating etc)
 *   * Managing the build dependencies
 *   * Actually handling all the dirtyness of execution
 *   * Handling failures
 *   
 * But I can't see how to disentangle them.
 */
public class BuildContext {
	private BuildOrder buildOrder;

	public static class ComparisonResource extends SolidResource {
		private final String comparison;

		public ComparisonResource(String from) {
			super(null, new File(FileUtils.getCurrentDir(), "unused"));
			this.comparison = from;
		}

		@Override
		public Strategem getBuiltBy() {
			throw new UtilException("Not implemented");
		}

		@Override
		public File getPath() {
			throw new UtilException("Not implemented");
		}

		@Override
		public String compareAs() {
			return comparison;
		}

	}

	private DependencyManager manager;
	private final Config conf;
	
	// TODO: this should be more general "deferred failure"
	private final List<JUnitFailure> failures = new ArrayList<JUnitFailure>();
	private Date buildStarted;
	private int totalErrors;
	private boolean buildBroken;
	private int projectsWithTestFailures;
	private List<Strategem> strats = new ArrayList<Strategem>();
	private ExecuteStrategem currentStrat;
	private final List<Pattern> showArgsFor = new ArrayList<Pattern>();
	private final List<Pattern> showDebugFor = new ArrayList<Pattern>();

	public BuildContext(Config conf, ConfigFactory configFactory, boolean buildAll, List<String> showArgsFor, List<String> showDebugFor) {
		this.conf = conf;
		buildOrder = new BuildOrder(conf, buildAll);
		manager = new DependencyManager(conf, buildOrder);
		for (String s : showArgsFor)
			this.showArgsFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (String s : showDebugFor)
			this.showDebugFor.add(Pattern.compile(".*"+s.toLowerCase()+".*"));
		for (Nature n : configFactory.installedNatures())
			registerNature(n.getClass(), n);
		for (Strategem s : conf.getStrategems())
			strats.add(s);
	}
	
	public void configure()
	{
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

	}
	
	public BuildStatus execute(ItemToBuild itb) {
		if (itb.needsBuild == BuildStatus.SKIPPED)  // defer now, do later ...
			System.out.print("-");
		else if (itb.needsBuild == BuildStatus.SUCCESS) // normal build
			System.out.print("*");
		else if (itb.needsBuild == BuildStatus.DEFERRED) // was deferred, do now ...
			System.out.print("+");
		else if (itb.needsBuild == BuildStatus.RETRY) // just literally failed ... retrying
			System.out.print("!");
		else if (itb.needsBuild == BuildStatus.CLEAN) // is clean, that's OK
			System.out.print(" ");
		else
			throw new RuntimeException("Cannot handle status " + itb.needsBuild);
		
		System.out.println(" " + itb.id + ": " + itb.label);
		if (!itb.needsBuild.needsBuild())
			return itb.needsBuild;

		// Record when first build started
		if (buildStarted == null)
			buildStarted = new Date();
		BuildStatus ret = BuildStatus.BROKEN;
		try
		{
			ret = itb.tactic.execute(this, showArgs(itb.tactic), showDebug(itb.tactic));
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace(System.out);
		}
		if (ret.needsRebuild())
			buildOrder.forceRebuild();
		
		/* TODO: Somebody should do this ...
		// Test the contract when the strategem comes to an end
		else if (ret.builtResources() && currentCommands != null && !currentCommands.hasNext())
		{
			List<BuildResource> fails = new ArrayList<BuildResource>();
			for (BuildResource br : itb.belongsTo().buildsResources())
				if (!manager.isResourceAvailable(br))
					fails.add(br);
			if (!fails.isEmpty())
			{
				System.out.println("The strategem " + itb.belongsTo() + " failed in its contract to build " + fails);
				// This code should be abstracted out too ... I think we need another wrapper layer.
				buildOrder.forceRebuild();
				return BuildStatus.BROKEN;
			}
		}
		*/
		return ret;
	}

	public void tellMeAbout(Nature nature, Class<? extends BuildResource> cls) {
		manager.tellMeAbout(nature, cls);
	}

	private boolean showArgs(Tactic bc) {
		for (Pattern p : showArgsFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	private boolean showDebug(Tactic bc) {
		for (Pattern p : showDebugFor)
			if (p.matcher(bc.toString().toLowerCase()).matches())
				return true;
		return false;
	}

	public void junitFailure(JUnitRunCommand cmd, String stdout, String stderr) {
		JUnitFailure failure = new JUnitFailure(cmd, stdout, stderr);
		failures.add(failure);
		projectsWithTestFailures++;
	}

	public void showAnyErrors() {
		for (JUnitFailure failure : failures)
			failure.show();
		if (buildBroken)
		{
			System.out.println("!!!! BUILD FAILED !!!!");
			System.exit(1);
		}
		else if (buildStarted == null) {
			System.out.println("Nothing done.");
		} else
		{
			System.out.print(">> Build completed in ");
			System.out.print(DateUtils.elapsedTime(buildStarted, new Date(), DateUtils.Format.hhmmss3));
			if (projectsWithTestFailures > 0)
				System.out.print(" " + projectsWithTestFailures + " projects had test failures");
			if (totalErrors > 0)
				System.out.print(" " + totalErrors + " total build commands failed (including retries)");
			System.out.println();
		}
	}

	public void buildFail(BuildStatus outcome) {
		totalErrors++;
		System.out.println("Counting this as a failure - total so far: " + totalErrors);
		if (outcome.isBroken())
			buildBroken = true;
	}

	public File getPath(String name) {
		return conf.getPath(name);
	}

	public void registerNature(Class<?> cls, Nature n) {
		if (n instanceof BuildContextAware)
			((BuildContextAware)n).provideBuildContext(this);
	}

	public <T extends Nature> T getNature(Class<T> cls) {
		return conf.getNature(cls);
	}
	
	public void doBuild() {
		System.out.println("");
		System.out.println("Building ...");
		ItemToBuild bc;
		while ((bc = buildOrder.next())!= null)
		{
			BuildStatus outcome = execute(bc);
			if (!outcome.isGood())
			{
				buildFail(outcome);
				if (outcome.isBroken())
				{
					System.out.println("Aborting build due to failure");
					break;
				}
				else if (outcome.tryAgain())
				{
					System.out.println("  Failed ... retrying");
					buildOrder.tryAgain();
					continue;
				}
				// else move on ...
			}
			buildOrder.advance();
		}
		manager.saveDependencies();
		buildOrder.saveBuildOrder();
		showAnyErrors();
	}

	// Dumb delegate methods ...
	public Config getConfig() {
		return conf;
	}

	public BuildResource getPendingResource(PendingResource s) {
		return manager.getPendingResource(s);
	}

	public void resourceAvailable(BuildResource r) {
		manager.resourceAvailable(r);
	}

	public Iterable<BuildResource> getDependencies(Strategem parent) {
		return manager.getDependencies(parent);
	}

	public <T extends BuildResource> Iterable<BuildResource> getResources(Class<T> cls) {
		return manager.getResources(cls);
	}

	public boolean addDependency(Strategem dependent, BuildResource resource) {
		if (dependent == null)
			throw new QuickBuildException("The strategem cannot be null");
		if (resource == null)
			throw new QuickBuildException("The resource cannot be null");
		return manager.addDependency(dependent, resource);
	}
	
	public void saveDependencies() {
		manager.saveDependencies();
	}

	public void saveBuildOrder() {
		buildOrder.saveBuildOrder();
	}

	public String printableDependencyGraph() {
		return manager.printableDependencyGraph();
	}

	@SuppressWarnings("unchecked")
	public <T extends BuildResource> T getBuiltResource(Strategem p, Class<T> ofCls) {
		for (BuildResource br : p.buildsResources())
			if (ofCls.isInstance(br))
				return (T)br;
		throw new QuickBuildException("There is no resource of type " + ofCls + " produced by " + p.identifier());
	}

	public String printableBuildOrder() {
		return buildOrder.printOut();
	}
}
