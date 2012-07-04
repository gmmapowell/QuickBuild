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
		System.exit((lsnr.failed>100)?100:lsnr.failed);
	}
}
