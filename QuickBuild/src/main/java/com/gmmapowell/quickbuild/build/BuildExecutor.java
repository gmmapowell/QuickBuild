package com.gmmapowell.quickbuild.build;

import java.util.Date;

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
	private boolean haveCompletedUpTo = false;

	public BuildExecutor(BuildContext cxt, boolean debug) {
		this.cxt = cxt;
		this.debug = debug;
		buildOrder = cxt.getBuildOrder();
		ehandler = cxt.getErrorHandler();
		manager = cxt.getDependencyManager();
		rm = cxt.getResourceManager();
	}

	public void doBuild() {
		if (!cxt.quietMode())
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
					fatal(itb);
					continue;
				}
				else if (outcome.tryAgain())
				{
					if (debug)
						System.out.println("  Failed ... returning to ready queue");
					returnToWell(itb);
//					System.out.println(cxt.printableBuildOrder(false));
					continue;
				}
				else
					System.out.println("  Partially failed, moving on ...");
				// else move on ...
			}
			advance();
		}
		manager.saveDependencies();
		buildOrder.saveBuildOrder();
		showAnyErrors();
		buildOrder.commitAll();
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
		buildOrder.reject(itb.tactic, true);
		status = Status.REJECT_AND_SEARCH_WELL;
	}
	
	public void showAnyErrors() {
		boolean buildBroken = ehandler.showLog(System.err);
		if (buildBroken)
		{
			if (projectsWithTestFailures > 0)
				System.err.print(" " + projectsWithTestFailures + " projects had test failures");
			System.err.println("!!!! BUILD FAILED !!!!");
			System.exit(1);
		}
		else if (buildStarted == null) {
			System.err.println("Nothing done.");
		} else
		{
			System.err.print(">> Build completed in ");
			System.err.print(DateUtils.elapsedTime(buildStarted, new Date(), DateUtils.Format.hhmmss3));
			if (projectsWithTestFailures > 0)
				System.err.print(" " + projectsWithTestFailures + " projects had test failures");
			if (totalErrors > 0)
				System.err.print(" " + totalErrors + " total build commands failed (including retries)");
			System.err.println();
		}
	}

	
	public BuildStatus execute(ItemToBuild itb) {
		itb.announce(!cxt.quietMode(), currentTactic);
		if (haveCompletedUpTo  || !itb.needsBuild.needsBuild())
		{
			itb.export(rm);
			if (cxt.upTo != null && itb.id.equals("Jar["+cxt.upTo+"-jar]"))
				haveCompletedUpTo = true;
			return itb.needsBuild;
		}

		// Record when first build started
		if (buildStarted == null)
			buildStarted = new Date();
		BuildStatus ret = BuildStatus.BROKEN;
		try
		{
			ret = itb.tactic.execute(cxt, cxt.showArgs(itb.tactic), cxt.showDebug(itb.tactic));
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace(System.out);
		}
		if (ret.needsRebuild())
			itb.fail();
		else if (ret.isGood()) {
			itb.commitAll();
			itb.export(rm);
			buildOrder.saveBuildOrder();
			manager.saveDependencies();
		}
		
		return ret;
	}
}
