package com.gmmapowell.system;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class RunProcess {

	private final List<String> cmdarray = new ArrayList<String>();
	private File workingDir = null;
	private ThreadedStreamReader stdout;
	private ThreadedStreamReader stderr;
	private int exitCode;
	private ByteArrayOutputStream outCapture;
	private ByteArrayOutputStream errCapture;
	private boolean finished;
	private boolean showArgs;

	public RunProcess(String cmd) {
		cmdarray.add(cmd);
		stdout = new ThreadedStreamReader();
		stderr = new ThreadedStreamReader();
	}

	public void arg(String string) {
		cmdarray.add(string);
	}

	public void redirectStdout(OutputStream out) {
		stdout = new ThreadedStreamReader(out);
	}

	public void redirectStderr(OutputStream err) {
		stderr = new ThreadedStreamReader(err);
	}
	
	public void discardStdout() {
		stdout = new ThreadedStreamReader();
	}
	
	public void discardStderr() {
		stderr = new ThreadedStreamReader();
	}
	
	public void captureStdout() {
		outCapture = new ByteArrayOutputStream();
		stdout = new ThreadedStreamReader(outCapture);
	}
	
	public void captureStderr() {
		errCapture = new ByteArrayOutputStream();
		stderr = new ThreadedStreamReader(errCapture);
	}

	public void showArgs(boolean b)
	{
		showArgs = b;
	}
	
	public void executeInDir(File inDir) {
		workingDir = inDir;
	}
	
	public void execute()
	{
		if (showArgs)
			for (String s : cmdarray)
				System.out.println(s);
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray.toArray(new String[cmdarray.size()]), null, workingDir);
			stdout.read(proc.getInputStream());
			stderr.read(proc.getErrorStream());
			exitCode = proc.waitFor();
			stdout.join();
			stderr.join();
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
		finished = true;
	}
	
	public int getExitCode()
	{
		assertFinished();
		return exitCode;
	}
	
	public String getStdout()
	{
		assertFinished();
		if (outCapture == null)
			throw new UtilException("Can only call this is output was captured during setup");
		return outCapture.toString();
	}
	
	public String getStderr() {
		assertFinished();
		if (errCapture == null)
			throw new UtilException("Can only call this is errors were captured during setup");
		return errCapture.toString();
	}
	
	public Reader stdoutReader() {
		return new StringReader(getStdout());
	}
	
	public Reader stderrReader() {
		return new StringReader(getStderr());
	}

	private void assertFinished() {
		if (!finished)
			throw new UtilException("Can only call this method after successful completion");
	}

	public void debug() {
		showArgs(true);
		redirectStderr(System.out);
		redirectStdout(System.out);
	}
}

