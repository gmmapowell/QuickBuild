package com.gmmapowell.test;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.gmmapowell.utils.DateUtils.Format;

public class JUnitListener extends RunListener {

	public int failed;

	@Override
	public void testRunStarted(Description description) throws Exception {
		System.err.println();
		System.err.println("Starting batch " + description);
	}
	
	@Override
	public void testStarted(Description description) throws Exception {
		System.err.println();
		System.err.println("Starting test " + description);
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		System.err.println();
		System.err.println("!! FAILED");
		System.err.println(failure.getMessage());
		System.err.println(failure.getTrace());
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		System.err.println();
		System.err.println("Starting test " + description);
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		System.err.println("Run finished in " + Format.hhmmss3.format(result.getRunTime()));
		System.out.println("Ran: " + result.getRunCount()+": "+result.getFailureCount()+" failed ("+result.getIgnoreCount()+" ignored)");
		failed += result.getFailureCount();
		for (Failure x : result.getFailures())
			System.out.println(x);
	}
}
