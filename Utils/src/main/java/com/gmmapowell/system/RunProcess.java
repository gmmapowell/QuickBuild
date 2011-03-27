package com.gmmapowell.system;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class RunProcess {

	private final List<String> cmdarray = new ArrayList<String>();
	private File workingDir = null;
	private ThreadedStreamReader stdout;
	private ThreadedStreamReader stderr;
	private int exitCode;

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
	
	public void execute()
	{
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
	}
}

