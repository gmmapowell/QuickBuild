package com.gmmapowell.quickbuild.build;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.exceptions.CycleDetectedException;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.DateUtils;

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
		Tactic tactic = null;
		for (ItemToBuild itb : buildOrder) {
			if (itb.id.contains(upTo)) {
				tactic = itb.tactic;
				break;
			}
		}
			
		if (tactic == null) {
			System.out.println("Upto target " + upTo + " not found in build order");
			return null;
		}
		ret.add(tactic);
		Iterable<BuildResource> dependencies = cxt.getDependencyManager().getDependencies(tactic);
		for (BuildResource br : dependencies) {
			if (br instanceof ProcessResource) {
				ProcessResource pr = (ProcessResource) br;
				ret.add(pr.getBuiltBy());
			}
		}

		return ret;
	}

	public void doBuild() {
		if (!cxt.quietMode() && !cxt.output.forTeamCity())
			System.out.println("Building ...");
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
			if (!outcome.isGood())
			{
				if (outcome.isWorthReporting())
					ehandler.buildFail(outcome);
				if (outcome.isBroken())
				{
					System.out.println("  Failed ... pressing on to the grand fallacy");
					cxt.grandFallacy = true;
					brokenTactics.add(itb.tactic);
					fatal(itb);
					continue;
				}
				else if (outcome.tryAgain())
				{
					if (debug)
						System.out.println("  Failed ... returning to ready queue");
					if (itb.hasUnbuiltDependencies(cxt.getBuildOrder()))
						returnToWell(itb);
					else
						status = Status.REJECT_AND_SEARCH_WELL;
//					System.out.println(cxt.printableBuildOrder(false));
					continue;
				}
				else if (outcome.partialFail())
					System.out.println("  Partially failed, moving on ...");
				// else move on ...
			}
			advance();
		}
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
				status = Status.NOT_SET;
				return itb;
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
		buildOrder.reject(itb.tactic, false);
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
		if (hasBrokenDependencies(itb)) {
			itb.announce(cxt.output, !cxt.quietMode(), currentTactic, BuildStatus.BROKEN_DEPENDENCIES);
			cxt.output.finishBuildStep();
			return BuildStatus.BROKEN_DEPENDENCIES;
		}
		if (!itb.needsBuild.needsBuild())
		{
			itb.export(cxt.output, rm);
			itb.announce(cxt.output, !cxt.quietMode(), currentTactic, itb.needsBuild);
			cxt.output.finishBuildStep();
			return itb.needsBuild;
		}
		else if (!isOnCriticalPath(itb))
		{
			itb.export(cxt.output, rm);
			itb.announce(cxt.output, !cxt.quietMode(), currentTactic, BuildStatus.NOTCRITICAL);
			itb.revert();
			cxt.output.finishBuildStep();
			return BuildStatus.NOTCRITICAL;
		}
		itb.considerAutoSkipping(cxt);
		itb.announce(cxt.output, !cxt.quietMode(), currentTactic, itb.needsBuild);

		// Record when first build started
		if (buildStarted == null)
			buildStarted = new Date();
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
		cxt.output.finishBuildStep();
		if (ret.needsRebuild())
			itb.fail();
		else if (ret.isGood()) {
			itb.commitAll();
			itb.export(cxt.output, rm);
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
