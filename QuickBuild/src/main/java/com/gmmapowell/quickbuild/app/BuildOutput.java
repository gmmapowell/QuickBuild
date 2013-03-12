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
			System.out.println("##teamcity[testSuiteStarted name='" + escape(string) +"']");
	}

	public void endTestBatch(String string) {
		if (teamCity)
			System.out.println("##teamcity[testSuiteFinished name='" + escape(string) +"']");
	}

	public void testSummary(String string) {
		if (!teamCity)
			System.out.println(string);
	}

	public void startTest(String currentTest) {
		if (teamCity)
			System.out.println("##teamcity[testStarted name='" + escape(currentTest) +"']");
	}

	public void finishTest(String currentTest) {
		if (teamCity)
			System.out.println("##teamcity[testFinished name='" + escape(currentTest) +"']");
	}

	public void failTest(String string) {
		if (teamCity)
			System.out.println("##teamcity[testFailed name='" + escape(string) +"']");
	}

	public void startBuildStep(String cmd, String invocation) {
		if (teamCity) {
			buildCmd = cmd;
			if (cmd.equals("JUnit")) {
				openBlock("JUnit");
			} else {
				System.out.println("##teamcity[compilationStarted compiler='" + escape(cmd) + "']");
				System.out.println("##teamcity[message text='" + escape(invocation) + "']");
			}
		} else
			System.out.println(invocation);
	}

	public void finishBuildStep() {
		if (teamCity) {
			if (buildCmd.equals("JUnit"))
				closeBlock("JUnit");
			else {
				System.out.println("##teamcity[compilationFinished compiler='" + escape(buildCmd) + "']");
				buildCmd = null;
			}
		}
	}

	public void buildErrors(String errors) {
		if (teamCity) {
			String[] messages = errors.split("\n");
			for (String m : messages)
				System.out.println("##teamcity[message text='" + escape(m) + "' status='ERROR']");
		} else {
			System.out.println("!!! Errors were detected in javac, but could not be corrected:");
			System.out.println(errors);
		}
	}

	private String escape(String m) {
		return m.replaceAll("\\|", "||").replaceAll("\\[", "|[").replaceAll("\\]", "|]").replaceAll("\n", "|n");
	}

	public void complete(String identifier) {
		if (!teamCity)
			System.out.println("       Completing " + identifier);
	}
}
