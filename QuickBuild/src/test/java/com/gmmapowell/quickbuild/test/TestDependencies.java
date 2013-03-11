package com.gmmapowell.quickbuild.test;

import org.junit.Test;

public class TestDependencies {

	@Test
	public void testABCD() {
		runQB("abcd", 3,
				new String[] { "buildOrder.xml", "dependencies.xml" /* git files */ },
				new String[] { "buildOrder.xml", "dependencies.xml" /* git files */ },
				new String[] { "buildOrder.xml", "dependencies.xml" /* git files */ }
				);
	}

	private void runQB(String name, int reps, String[]... expectedFiles) {
		for (int i=1;i<=reps;i++) {
			System.out.println("Running " + name + " for the " + i + " time");
			// TODO: run qb
			// TODO: check golden file
			// TODO: check if files should be deleted
		}
	}

}
