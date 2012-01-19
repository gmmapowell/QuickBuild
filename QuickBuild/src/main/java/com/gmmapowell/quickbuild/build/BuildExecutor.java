package com.gmmapowell.quickbuild.build;

import java.util.Date;

import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.DateUtils;

public class BuildExecutor {
	private enum Status { NOT_SET, BUILD_CURRENT, RESTART_BAND, RESTART_STRAT, NEXT_TACTIC, NEXT_STRAT, NEXT_BAND };

	private final BuildContext cxt;
	private BuildOrder buildOrder;
	private ErrorHandler ehandler;
	private boolean isBroken = false;
	private DependencyManager manager;
	private ResourceManager rm;

	private Date buildStarted;
	private int totalErrors;
	private int projectsWithTestFailures;

	private Status status = Status.BUILD_CURRENT;
	private int currentBand = 0;
	private int currentStrat = 0;
	private int currentTactic = 0;
	private final boolean debug;


	public BuildExecutor(BuildContext cxt, boolean debug) {
		this.cxt = cxt;
		this.debug = debug;
		buildOrder = cxt.getBuildOrder();
		ehandler = cxt.getErrorHandler();
		manager = cxt.getDependencyManager();
		rm = cxt.getResourceManager();
	}

	public void doBuild() {
		System.out.println("Building ...");
		ItemToBuild itb;
		while ((itb = next())!= null)
		{
			ehandler.currentCmd(itb);
			if (debug)
			{
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
					System.out.println("  Failed ... skipping to next in band");
					fatal();
					continue;
				}
				else if (outcome.tryAgain())
				{
					System.out.println("  Failed ... returning to ready queue");
					tryAgain();
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
//			System.out.println("next(status=" + status + ")");
			if (status == Status.NOT_SET)
				throw new QuickBuildException("Invalid status");
			// Do the obvious first: try and move on if required
			if (status == Status.NEXT_TACTIC)
				currentTactic++;
			else if (status == Status.RESTART_BAND)
			{
				currentTactic = 0;
				currentStrat = 0;
				status = Status.BUILD_CURRENT;
			}
			else if (status == Status.RESTART_STRAT)
			{
				currentTactic = 0;
				status = Status.BUILD_CURRENT;
			}
			else if (status == Status.NEXT_STRAT)
			{
				currentTactic = 0;
				currentStrat++;
			}
			else if (status == Status.NEXT_BAND)
			{
				currentTactic = 0;
				currentStrat = 0;
				currentBand++;
			}

//			System.out.println("itb("+currentBand+","+currentStrat+","+currentTactic+")");
			// If the identified ITB exists, return it
			ItemToBuild itb = buildOrder.get(currentBand, currentStrat, currentTactic);
			if (itb != null)
			{
				status = Status.NOT_SET;
				return itb;
			}

//			System.out.println("itb = null, status=" + status);
			
			// OK, we've reached the end of the road ...
			if (status == Status.BUILD_CURRENT)
			{
				// This is a random hack to try and stay out of infinite loops ...
				if (retrying)
					throw new QuickBuildException("Can't build current when there isn't one! " + currentBand + " " + currentStrat + " " + currentTactic);

				status = Status.NEXT_TACTIC;
				retrying = true;
			}
			else if (status == Status.NEXT_BAND)
				return null; // we are at the beginning of a band, but none ...
			else if (status == Status.NEXT_STRAT)
			{
				if (isBroken)
					return null;
				status = Status.NEXT_BAND;
			}
			else if (status == Status.NEXT_TACTIC)
				status = Status.NEXT_STRAT;
		}
	}

	public void advance() {
		status = Status.NEXT_TACTIC;
	}

	public void tryAgain() {
		status = Status.RESTART_STRAT;
	}

	public void fatal() {
		isBroken  = true;
		status = Status.NEXT_STRAT;
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
		if (itb.firstTactic())
			System.out.println(itb.name());
		if (itb.needsBuild == BuildStatus.NOTAPPLICABLE) 
			System.out.print("v");
		else if (itb.needsBuild == BuildStatus.SKIPPED)  // defer now, do later ...
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
		{
			if (itb.lastTactic() && itb.strat instanceof ExecuteStrategem)
			{
				rm.exportAll((ExecuteStrategem) itb.strat);
			}
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
		if (itb.lastTactic() && itb.strat instanceof ExecuteStrategem)
			rm.stratComplete(ret, ((ExecuteStrategem)itb.strat).getStrat());
		if (ret.needsRebuild())
			itb.strat.fail();
		
		return ret;
	}
}
