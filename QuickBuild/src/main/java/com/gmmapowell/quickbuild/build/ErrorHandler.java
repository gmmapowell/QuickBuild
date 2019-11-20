package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.zinutils.utils.FileUtils;

import com.gmmapowell.utils.PrettyPrinter;

/**
 * The model for the error handler is as follows:
 * <ul>
 * <li>One error handler is created at the beginning of execution;
 * <li>On each tactic initialization, "currentCmd" is called with the current context
 * <li>If the tactic has built-in error handling it will call the "failure" method is called with the arguments, stdout and stderr from the command when it detects an error
 * <li>This will return an "ErrorCase" object which is pre-filled with the case information, and then can have specific messages attached
 * <li>The build infrastructure automatically calls "buildFail" with the outcome
 * <li>At the end of execution, the error log (if any) is printed.
 * <li>Detailed information is available in qb/project/logs/case
 * </ul>
 *
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ErrorHandler {

	private ItemToBuild itb;
	private ErrorCase currentCase;
	private final List<ErrorCase> cases = new ArrayList<ErrorCase>();
	private File logDir;

	public ErrorHandler(File logDir)
	{
		this.logDir = logDir;
		FileUtils.cleanDirectory(logDir);
	}
	
	public void currentCmd(int currentTactic, ItemToBuild itb) {
		this.itb = itb;
		this.currentCase = null;
	}
	
	public ErrorCase failure(List<String> args, String stdout, String stderr) {
		getCase(args, stdout, stderr);
		return currentCase;
	}

	public void buildFail(BuildStatus outcome) {
		getCase(null, null, null);
		currentCase.flush(outcome);
	}
	
	private void getCase(List<String> args, String stdout, String stderr) {
		if (currentCase == null)
		{
			currentCase = new ErrorCase(logDir, itb, args, stdout, stderr);
			cases.add(currentCase);
		}
	}
	
	public boolean showLog(PrintStream err) {
		boolean buildBroken = false;
		Collections.sort(cases, ErrorCase.Comparator);
		PrettyPrinter pp = new PrettyPrinter();
		for (ErrorCase c : cases)
		{
			c.summary(pp);
			if (c.isBroken())
				buildBroken = true;
		}
		err.print(pp);
		return buildBroken;
	}
	
}
