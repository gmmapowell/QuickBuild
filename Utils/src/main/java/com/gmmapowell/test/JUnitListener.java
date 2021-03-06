package com.gmmapowell.test;

import java.io.File;
import java.util.Date;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

import com.gmmapowell.utils.DateUtils.Format;

public class JUnitListener extends RunListener {
	private Date batchStartTime;
	private Date testStartTime;
	int runCount;
	int failed;
	int ignored;
	int startRun;
	int startFailed;
	int startIgnored;
	private Description batch;
	boolean inTest = false;
	private XML toXML;
	private File xmlFile;
	private XMLElement currXMLTest;

	public void toXML(String arg) {
		xmlFile = new File(arg);
		toXML = XML.create("1.0", "testsuite");
	}

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
		if (toXML != null) {
			currXMLTest = toXML.top().addElement("testcase");
			currXMLTest.setAttribute("classname", description.getClassName());
			currXMLTest.setAttribute("name", description.getMethodName());
		}
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			testIgnored(failure.getDescription());
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
			if (toXML != null) {
				XMLElement fail = currXMLTest.addElement("failure");
				fail.setAttribute("type", failure.getException().getClass().getName());
				fail.addText(failure.getTrace());
			}
			failed++;
			inTest = false;
		} else {
			System.out.println("Saw repeated (or setup/teardown) failure in " + failure.getDescription());
			System.err.println(failure.getMessage());
			System.err.println(failure.getTrace());
			if (failed == 0) { // make sure it's at least slightly red
				runCount++;
				failed++;
			}
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
		// In the degenerate case, we can be called even if start wasn't
		if (batchStartTime != null)
			System.out.println("Ran batch " + batch + ": " + (runCount-startRun)+" total, "+(failed-startFailed)+" failed ("+(ignored-startIgnored)+" ignored) finished in " + Format.hhmmss3.format(new Date().getTime()-batchStartTime.getTime()));
		
		// this will happen multiple times, but it should give cumulative results
		if (xmlFile != null) {
			XMLElement top = toXML.top();
			top.setAttribute("errors", "0");
			top.setAttribute("failures", Integer.toString(failed));
			top.setAttribute("tests", Integer.toString(runCount));
			toXML.write(xmlFile);
		}
	}

}
