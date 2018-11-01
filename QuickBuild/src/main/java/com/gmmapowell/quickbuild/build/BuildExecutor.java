package com.gmmapowell.quickbuild.build;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.zinutils.exceptions.CycleDetectedException;

import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.utils.DateUtils;

public class BuildExecutor {
	private enum Status { NOT_SET, NEXT_TACTIC, REJECT_AND_SEARCH_WELL };

	private final BuildContext cxt;
	private BuildOrder buildOrder;
	private ErrorHandler ehandler;
	private DependencyManager manager;
	private ResourceManager rm;

	private Date buildStarted;
	private int totalErrors;
	private int projectsWithTestFailures;

	private Status status = Status.REJECT_AND_SEARCH_WELL;
	private int currentTactic = 0;
	private final boolean debug;
	private Set<Tactic> upTo = null;
	private Set<Tactic> brokenTactics = new HashSet<Tactic>();

	public BuildExecutor(BuildContext cxt, boolean debug) {
		this.cxt = cxt;
		this.debug = debug;
		buildOrder = cxt.getBuildOrder();
		ehandler = cxt.getErrorHandler();
		manager = cxt.getDependencyManager();
		rm = cxt.getResourceManager();
		if (cxt.upTo != null)
			upTo = figureNecessarySteps(cxt.upTo);
	}

	private Set<Tactic> figureNecessarySteps(String upTo) {
		Set<Tactic> ret = new HashSet<Tactic>();
		Set<Tactic> newTactics = new HashSet<Tactic>();
		for (ItemToBuild itb : buildOrder) {
			if (itb.id.contains(upTo)) {
				newTactics.add(itb.tactic);
				break;
			}
		}
			
		if (newTactics.isEmpty()) {
			System.out.println("Upto target " + upTo + " not found in build order");
			return null;
		}
		while (!newTactics.isEmpty()) {
			Set<Tactic> iterateOver = newTactics;
			newTactics = new HashSet<Tactic>();
			for (Tactic t : iterateOver) {
				ret.add(t);
				for (Tactic dt : t.getProcessDependencies())
					ret.add(dt);
				Iterable<BuildResource> dependencies = cxt.getDependencyManager().getDependencies(t);
				for (BuildResource br : dependencies) {
					if (br.getBuiltBy() != null) {
						newTactics.add(br.getBuiltBy());
					}
				}
			}
		}
		System.out.println("Only building critical path for " + upTo +":\n" + ret);
		return ret;
	}

	public void doBuild() {
		if (!cxt.quietMode() && !cxt.output.forTeamCity())
			System.out.println("Building ...");
		cxt.setConfigVar("buildStatus", BuildStatus.SUCCESS.name());
		ItemToBuild itb;
		while ((itb = next())!= null)
		{
			ehandler.currentCmd(currentTactic, itb);
			if (debug)
			{
				System.out.println(itb.tactic);
				System.out.print(new Date().toString()+" ");
			}
			BuildStatus outcome = execute(itb);
			if (debug)
			{
				System.out.println(new Date().toString() + " Completed");
			}
			if (outcome.isGood()) {
				buildOrder.completeTactic(itb.tactic);
			}
			else
			{
				if (outcome.isWorthReporting())
					ehandler.buildFail(outcome);
				if (outcome.isBroken())
				{
					buildOrder.completeTactic(itb.tactic);
					if (!cxt.grandFallacyMode() || outcome.isReallyFatal())
						break;
					cxt.setConfigVar("buildStatus", outcome.name());
					System.out.println("  Failed ... pressing on to the grand fallacy");
					cxt.grandFallacy = true;
					brokenTactics.add(itb.tactic);
					fatal(itb);
					continue;
				}
				else if (outcome.tryAgain())
				{
					if (debug)
						System.out.println("  Failed but made corrections ... returning to ready queue");
					if (itb.hasUnbuiltDependencies(cxt.getBuildOrder()))
						returnToWell(itb);
					else
						status = Status.REJECT_AND_SEARCH_WELL;
//					System.out.println(cxt.printableBuildOrder(false));
					continue;
				}
				else if (outcome.tryLater()) {
					if (debug)
						System.out.println("  Failed with possible future corrections ... returning to bottom of ready queue");
					returnToBottomOfWell(itb);
					status = Status.REJECT_AND_SEARCH_WELL;
//					System.out.println(cxt.printableBuildOrder(false));
					continue;
				}
				else if (outcome.partialFail()) {
					cxt.setConfigVar("buildStatus", outcome.name());
					System.out.println("  Partially failed, moving on ...");
					buildOrder.completeTactic(itb.tactic);
				}
			}
			advance();
		}
		cxt.waitForBackgroundsToComplete();
		manager.saveDependencies();
		buildOrder.saveBuildOrder();
		showAnyErrors();
//		buildOrder.revertRemainder();
	}

	public ItemToBuild next() {
		boolean retrying = false;
		for (;;)
		{
			if (debug)
				System.out.println("next(status=" + status + ")");
			if (status == Status.NOT_SET)
				throw new QuickBuildException("Invalid status");
			// Do the obvious first: try and move on if required
			if (status == Status.NEXT_TACTIC)
				currentTactic++;
			else if (status == Status.REJECT_AND_SEARCH_WELL)
				; // do nothing
			else
				throw new QuickBuildException("Invalid status: " + status);

			if (debug)
				System.out.println("itb("+currentTactic+")");
			// If the identified ITB exists, return it
			ItemToBuild itb = buildOrder.get(currentTactic);
			if (itb != null)
			{
				if (itb.hasUnbuiltDependencies(buildOrder)) {
					returnToWell(itb);
					continue;
				} else {
					status = Status.NOT_SET;
					return itb;
				}
			}

			if (debug)
				System.out.println("itb = null, status=" + status);
			
			// OK, we've reached the end of the road ...
			if (status == Status.REJECT_AND_SEARCH_WELL)
			{
				// This is a random hack to try and stay out of infinite loops ...
				if (retrying)
					throw new QuickBuildException("Can't build current when there isn't one! " + currentTactic);

				status = Status.NEXT_TACTIC;
				retrying = true;
			}
			else
				return null;
		}
	}

	public void advance() {
		status = Status.NEXT_TACTIC;
	}

	public void returnToWell(ItemToBuild itb) {
		buildOrder.reject(itb.tactic, -1);
		status = Status.REJECT_AND_SEARCH_WELL;
	}

	public void returnToBottomOfWell(ItemToBuild itb) {
		buildOrder.reject(itb.tactic, currentTactic);
		status = Status.REJECT_AND_SEARCH_WELL;
	}

	public void fatal(ItemToBuild itb) {
//		buildOrder.reject(itb.tactic, true);
		status = Status.NEXT_TACTIC;
	}
	
	public void showAnyErrors() {
		boolean buildBroken = ehandler.showLog(System.err);
		if (buildBroken)
		{
			if (projectsWithTestFailures > 0)
				System.err.println(" " + projectsWithTestFailures + " projects had test failures");
			System.err.println("!!!! BUILD FAILED !!!!");
			System.out.println("Elapsed time: " + DateUtils.elapsedTime(buildStarted, new Date(), DateUtils.Format.hhmmss3));
			System.exit(1);
		}
		else if (buildStarted == null) {
			System.out.println("Nothing done.");
		} else
		{
			StringBuilder sb = new StringBuilder();
			sb.append(">> Build completed in ");
			sb.append(DateUtils.elapsedTime(buildStarted, new Date(), DateUtils.Format.hhmmss3));
			if (projectsWithTestFailures > 0)
				sb.append(" " + projectsWithTestFailures + " projects had test failures");
			if (totalErrors > 0)
				sb.append(" " + totalErrors + " total build commands failed (including retries)");
			System.out.println(sb);
		}
	}

	
	public BuildStatus execute(ItemToBuild itb) {
		boolean verbose = !cxt.quietMode();
		if (itb.sentToBottomAt == currentTactic)
			return BuildStatus.LOOPING;
		if (hasBrokenDependencies(itb)) {
			itb.announce(cxt.output, verbose, currentTactic, BuildStatus.BROKEN_DEPENDENCIES);
			cxt.output.finishBuildStep();
			return BuildStatus.BROKEN_DEPENDENCIES;
		}
		BuildStatus stat = itb.needsBuild;
		if (itb.tactic instanceof AlwaysRunMe || (itb.tactic instanceof JUnitRunCommand && cxt.alwaysRunTests()))
			stat = BuildStatus.SUCCESS;
		if (!stat.needsBuild())
		{
			itb.export(cxt.output, rm, verbose);
			itb.announce(cxt.output, verbose, currentTactic, itb.needsBuild);
			cxt.output.finishBuildStep();
			itb.commitAll();
			return itb.needsBuild;
		}
		else if (!isOnCriticalPath(itb))
		{
			itb.export(cxt.output, rm, verbose);
			itb.announce(cxt.output, verbose, currentTactic, BuildStatus.NOTCRITICAL);
			itb.revert();
			cxt.output.finishBuildStep();
			return BuildStatus.NOTCRITICAL;
		}
		else if (itb.considerAutoSkipping(cxt)) {
			itb.announce(cxt.output, verbose, currentTactic, BuildStatus.NOTCRITICAL);
			itb.revert();
			cxt.output.finishBuildStep();
			return BuildStatus.NOTCRITICAL;
		}
		itb.announce(cxt.output, verbose, currentTactic, stat);

		// Record when first build started
		Date taskStarted = new Date();
		if (buildStarted == null)
			buildStarted = taskStarted;
		BuildStatus ret = BuildStatus.BROKEN;
		try
		{
			ret = itb.tactic.execute(cxt, cxt.showArgs(itb.tactic), cxt.showDebug(itb.tactic));
		}
		catch (CycleDetectedException ex) {
			manager.cleanFile();
			buildOrder.cleanFile();
			System.out.println(ex.getMessage());
			System.out.println("Cleaning out state files and exiting");
			System.exit(1);
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace(System.out);
		}
		finally {
			Date taskDone = new Date();
			if (taskDone.getTime() - taskStarted.getTime() > 5000) {
				System.out.println("  -- took " + DateUtils.elapsedTime(taskStarted, taskDone, DateUtils.Format.hhmmss3));
			}
		}
		cxt.output.finishBuildStep();
		if (ret.needsRebuild())
			itb.fail();
		else if (ret.isGood()) {
			itb.commitAll();
			itb.export(cxt.output, rm, verbose);
			buildOrder.saveBuildOrder();
			manager.saveDependencies();
		}
		
		return ret;
	}

	private boolean hasBrokenDependencies(ItemToBuild itb) {
		if (brokenTactics.isEmpty())
			return false;
		Iterable<BuildResource> dependencies = manager.getDependencies(itb.tactic);
		for (BuildResource br : dependencies) {
			if (br instanceof ProcessResource && brokenTactics.contains(((ProcessResource)br).getTactic()))
				return true;
		}
		return false;
	}

	private boolean isOnCriticalPath(ItemToBuild itb) {
		if (upTo == null)
			return true;
		else if (upTo.contains(itb.tactic))
			return true;
		return false;
	}
}
