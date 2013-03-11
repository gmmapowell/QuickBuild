package com.gmmapowell.test;

import org.junit.runner.JUnitCore;

public class QBJUnitRunner {
	public static void main(String... args)
	{
		JUnitCore runner = new JUnitCore();
		JUnitListener lsnr = new JUnitListener();
		runner.addListener(lsnr);
		for (String arg : args)
			try {
				runner.run(Class.forName(arg));
			} catch (ClassNotFoundException e) {
				System.err.println("There was no class " + arg + " found");
				lsnr.failed++;
			}
		
		// Restrict the exit code to be in a range 0-100 to avoid conflict with 128+
		int exitStatus = lsnr.failed;
		if (exitStatus > 100)
			exitStatus = 100;
		System.exit(exitStatus);
	}
}
