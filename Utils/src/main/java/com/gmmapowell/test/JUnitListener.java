package com.gmmapowell.test;

import java.util.Date;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.gmmapowell.utils.DateUtils.Format;

public class JUnitListener extends RunListener {
	private Date batchStartTime;
	private Date testStartTime;
	int runCount;
	int failed;
	int ignored;
	private int startRun;
	private int startFailed;
	private int startIgnored;
	private Description batch;
	boolean inTest = false;

	@Override
	public void testRunStarted(Description description) throws Exception {
		System.out.println("Running batch " + description);
		batch = description;
		batchStartTime = new Date();
		startRun = runCount;
		startFailed = failed;
		startIgnored = ignored;
	}

	
	@Override
	public void testStarted(Description description) throws Exception {
		System.err.println();
		System.err.println("Starting test " + description);
		System.out.println("Starting test " + description);
		testStartTime = new Date();
		runCount++;
		inTest = true;
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			testFailure(failure);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		if (inTest) {
			System.err.println("Failed test " + failure.getDescription());
			System.err.println(failure.getMessage());
			System.err.println(failure.getTrace());
			System.out.println("Failure: " + failure.getDescription());
			failed++;
			inTest = false;
		} else {
			System.out.println("Saw repeated failure in " + failure.getDescription());
		}
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		System.err.println();
		System.err.println("Ignoring test " + description);
		System.out.println("Ignoring test " + description);
		runCount++;
		ignored++;
		inTest = false; // I claim it is already
	}

	@Override
	public void testFinished(Description description) throws Exception {
		System.err.println("Finished test " + description);
		System.out.println("Duration: " + (new Date().getTime()-testStartTime.getTime()));
		inTest = false;
	}

	@Override
	public void testRunFinished(Result notUsed) throws Exception {
		System.out.println("Ran batch " + batch + ": " + (runCount-startRun)+" total, "+(failed-startFailed)+" failed ("+(ignored-startIgnored)+" ignored) finished in " + Format.hhmmss3.format(new Date().getTime()-batchStartTime.getTime()));
	}
}
