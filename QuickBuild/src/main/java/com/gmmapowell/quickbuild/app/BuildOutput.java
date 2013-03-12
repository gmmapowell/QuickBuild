package com.gmmapowell.quickbuild.app;

public class BuildOutput {
	private final boolean teamCity;
	private String buildCmd;

	public BuildOutput(boolean teamCity) {
		this.teamCity = teamCity;
	}

	public void println(Object o) {
		System.out.println(o);
	}

	public void openBlock(String string) {
		if (teamCity) {
			System.out.println("##teamcity[blockOpened name='" + string + "']");
		}
	}

	public void closeBlock(String string) {
		if (teamCity) {
			System.out.println("##teamcity[blockClosed name='" + string + "']");
		}
	}

	public boolean forTeamCity() {
		return teamCity;
	}

	public void cat(String block, String contents) {
		openBlock(block);
		System.out.print(contents);
		if (!contents.endsWith("\n"))
			System.out.println();
		closeBlock(block);
	}

	public void startTestBatch(String string) {
		if (teamCity)
			System.out.println("##teamcity[testSuiteStarted name='" + string +"']");
	}

	public void endTestBatch(String string) {
		if (teamCity)
			System.out.println("##teamcity[testSuiteFinished name='" + string +"']");
	}

	public void testSummary(String string) {
		if (!teamCity)
			System.out.println(string);
	}

	public void startTest(String currentTest) {
		if (teamCity)
			System.out.println("##teamcity[testStarted name='" + currentTest +"']");
	}

	public void finishTest(String currentTest) {
		if (teamCity)
			System.out.println("##teamcity[testFinished name='" + currentTest +"']");
	}

	public void failTest(String string) {
		if (teamCity)
			System.out.println("##teamcity[testFailed name='" + string +"']");
	}

	public void startBuildStep(String cmd, String invocation) {
		if (teamCity) {
			if (!cmd.equals("JUnit")) {
				buildCmd = cmd;
				System.out.println("##teamcity[compilationStarted compiler='" + cmd + "']");
				System.out.println("##teamcity[message text='" + invocation + "']");
			}
		} else
			System.out.println(invocation);
	}

	public void finishBuildStep() {
		if (teamCity && buildCmd != null) {
			System.out.println("##teamcity[compilationFinished compiler='" + buildCmd + "']");
			buildCmd = null;
		}
	}

	public void buildErrors(String errors) {
		if (teamCity) {
			String[] messages = errors.split("\n");
			for (String m : messages)
				System.out.println("##teamcity[message text='" + m + "' status='ERROR']");
		} else {
			System.out.println("!!! Errors were detected in javac, but could not be corrected:");
			System.out.println(errors);
		}
	}
}
