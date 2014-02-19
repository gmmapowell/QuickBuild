package com.gmmapowell.system;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;

public class RunProcess {

	private final List<String> cmdarray = new ArrayList<String>();
	private int preClassPos = -1;
	private boolean clearEnv;
	private final Map<String, String> envMap = new HashMap<String, String>();
	private File workingDir = null;
	private ThreadedStreamReader stdout;
	private ThreadedStreamReader stderr;
	private int exitCode;
	private ByteArrayOutputStream outCapture;
	private ByteArrayOutputStream errCapture;
	private boolean finished;
	private boolean showArgs;
	private boolean debug;
	private boolean runBackground;
	private Process proc;
	private Thread hook;

	public RunProcess(String cmd) {
		cmdarray.add(cmd);
		stdout = new ThreadedStreamReader();
		stderr = new ThreadedStreamReader();
	}

	public void arg(String string) {
		cmdarray.add(string);
	}

	public void preClassArg(String string) {
		if (preClassPos == -1)
			throw new UtilException("Can only use preClassArg with java command");
		cmdarray.add(preClassPos++, string);
	}

	public void clearEnv(boolean b) {
		clearEnv = b;
	}
	
	public void env(String key, String value) {
		if (key == null)
			return;
		else if (value == null)
			value = "";
		envMap.put(key, value);
	}

	public void env(String key, File dir) {
		env(key, dir.getPath());
	}

	public void env(String key, int k) {
		env(key, Integer.toString(k));
	}

	public void env(String key, boolean b) {
		env(key, Boolean.toString(b));
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
	
	public ThreadedStreamReader captureStdout() {
		outCapture = new ByteArrayOutputStream();
		stdout = new ThreadedStreamReader(outCapture);
		return stdout;
	}
	
	public ThreadedStreamReader captureStderr() {
		errCapture = new ByteArrayOutputStream();
		stderr = new ThreadedStreamReader(errCapture);
		return stderr;
	}

	public void showArgs(boolean b)
	{
		showArgs = b;
	}
	
	// TODO? Could have callback as well
	public void background(boolean b)
	{
		runBackground = b;
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
			ProcessBuilder builder = new ProcessBuilder(cmdarray);
			Map<String, String> env = builder.environment();
			if (clearEnv)
				env.clear();
			env.putAll(envMap);
			builder.directory(workingDir);
			proc = builder.start();
			stderr.echoStream(showArgs);
			stdout.read(proc.getInputStream());
			stderr.read(proc.getErrorStream());
			hook = new Thread() {
			  @Override
			  public void run() {
				  if (proc != null) {
					  proc.destroy();
					  proc = null;
				  }
			  }
			};
			Runtime.getRuntime().addShutdownHook(hook);
			if (runBackground)
			{
				new WaitForThread(this).start();
				return;
			}
			waitForEnd();
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	void waitForEnd() throws InterruptedException {
		exitCode = proc.waitFor();
		stdout.join();
		stderr.join();
		if (debug)
		{
			if (outCapture != null)
				System.out.println(outCapture);
			if (errCapture != null)
				System.out.println(errCapture);
		}
		if (debug)
			System.out.println("Process terminated");
		synchronized (this) {
			finished = true;
		}
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
			throw new UtilException("Can only call this if output was captured during setup");
		return outCapture.toString();
	}
	
	public String getStderr() {
		assertFinished();
		if (errCapture == null)
			throw new UtilException("Can only call this if errors were captured during setup");
		return errCapture.toString();
	}
	
	public Reader stdoutReader() {
		return new StringReader(getStdout());
	}
	
	public Reader stderrReader() {
		return new StringReader(getStderr());
	}

	private synchronized void assertFinished() {
		if (!finished)
			throw new UtilException("Can only call this method after successful completion");
	}

	public void debug(boolean showDebug) {
		if (showDebug)
		{
			debug = true;
			showArgs(true);
			redirectStderr(System.out);
			redirectStdout(System.out);
		}
	}

	public List<String> getArgs() {
		return cmdarray;
	}

	public synchronized boolean isFinished() {
		return finished;
	}

	public void kill() {
		// don't kill the dead
		if (finished || proc == null)
			return;
		if (debug)
			System.out.println("Killing " + proc);
		proc.destroy();
		while (true)
			try {
				exitCode = proc.waitFor();
				break;
			} catch (InterruptedException e) {
			}
		finished = true;
	}

	public static RunProcess childJava(Class<?> cls) {
		RunProcess ret = new RunProcess("java");
		ret.arg("-classpath");
		ret.arg(System.getProperty("java.class.path"));
		ret.preClassPos = ret.cmdarray.size();
		ret.arg(cls.getName());
		return ret;
	}
	
	public void destroy() {
		if (proc != null) {
			proc.destroy();
			proc = null;
		}
		if (hook != null) {
			Runtime.getRuntime().removeShutdownHook(hook);
			hook = null;
		}
	}
}
