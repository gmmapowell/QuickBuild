package com.gmmapowell.quickbuild.build;

import java.util.Date;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.DateUtils;

public class BuildExecutor {
	private final BuildContext cxt;
	enum Status { BEGIN, BUILD_THIS, MOVE_ON, RETRY, SKIP_TO_NEXT };
	Status status = Status.BEGIN;

	private Date buildStarted;
	private int totalErrors;
	private boolean buildBroken;
	private int projectsWithTestFailures;

	private int currentBand;
	private int currentStrat;
	private int currentTactic;

	private BuildOrder buildOrder;
	private ErrorHandler ehandler;
	private boolean isBroken = false;
	private DependencyManager manager;
	private ResourceManager rm;

	public BuildExecutor(BuildContext cxt) {
		this.cxt = cxt;
		buildOrder = cxt.getBuildOrder();
		ehandler = cxt.getErrorHandler();
		manager = cxt.getDependencyManager();
		rm = cxt.getResourceManager();
	}

	public void doBuild() {
		System.out.println("");
		System.out.println("Building ...");
		ItemToBuild itb;
		while ((itb = next())!= null)
		{
			ehandler.currentCmd(itb);
			BuildStatus outcome = execute(itb);
			if (!outcome.isGood())
			{
				ehandler.buildFail(outcome);
				if (outcome.isBroken())
				{
					System.out.println("  Failed ... skipping to next in band");
					fatal();
					continue;
				}
				else if (outcome.tryAgain())
				{
					System.out.println("  Failed ... retrying");
					tryAgain();
					continue;
				}
				// else move on ...
			}
			advance();
		}
		manager.saveDependencies();
		buildOrder.saveBuildOrder();
		showAnyErrors();
	}

	public ItemToBuild next() {
		for (;;)
		{
			if (status == Status.BEGIN)
			{
				currentBand = 0;
				currentStrat = 0;
				currentTactic = -1;
				status = Status.BUILD_THIS;
			}
			if (currentBand >= bands.size())
				return null;
			ExecutionBand band = bands.get(currentBand);
			if (status == Status.RETRY)
			{
				currentStrat = 0;
				currentTactic = 0;
			}
			if (currentStrat >= band.size())
			{
				if (isBroken)
					return null;
				currentBand++;
				currentStrat = 0;
				currentTactic = -1;
				continue;
			}
			BandElement be = band.get(currentStrat);
			if (status == Status.MOVE_ON || currentTactic == -1)
				currentTactic++;
			if (currentTactic >= be.size() || status == Status.SKIP_TO_NEXT)
			{
				currentStrat++;
				if (currentStrat < band.size())
					System.out.println("Advancing to " + band.get(currentStrat));
				currentTactic = -1;
				status = Status.BUILD_THIS;
				continue;
			}
			BuildStatus bs = BuildStatus.SUCCESS;
			Tactic tactic = be.tactic(currentTactic);
			if (be.isDeferred(tactic))
			{
				bs = BuildStatus.DEFERRED;
			}
			else if (be.isCompletelyClean())
				bs = BuildStatus.CLEAN;
			return new ItemToBuild(bs, be, tactic, (currentBand+1) + "." + (currentStrat+1)+"."+(currentTactic+1), tactic.toString());
		}
	}

	public void advance() {
		status = Status.MOVE_ON;
	}

	public void tryAgain() {
		status = Status.RETRY;
	}

	private ExecuteStrategem currentStrat() {
		ExecutionBand band = bands.get(currentBand);
		return (ExecuteStrategem) band.get(currentStrat);
	}

	public void fatal() {
		isBroken  = true;
		status = Status.SKIP_TO_NEXT;
	}
	

	public void showAnyErrors() {
		ehandler.showLog();
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
			forceRebuild();
		
		return ret;
	}
	
	public void forceRebuild() {
		cxt.getGitCacheFile(currentStrat(), "").delete();
	}
}
