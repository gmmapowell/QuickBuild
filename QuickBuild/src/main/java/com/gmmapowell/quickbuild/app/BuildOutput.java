package com.gmmapowell.quickbuild.app;

public class BuildOutput {
	private final boolean teamCity;

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
}
