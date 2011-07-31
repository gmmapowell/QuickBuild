package com.gmmapowell.apps;

public class Fixture implements Comparable<Fixture> {

	int home;
	int away;

	public Fixture(int home, int away) {
		this.home = home;
		this.away = away;
	}

	public void reverse() {
		int tmp = home;
		home = away;
		away = tmp;
	}

	@Override
	public int compareTo(Fixture other) {
		if (home > other.home)
			return 1;
		else if (other.home > home)
			return -1;
		else if (away > other.away)
			return 1;
		else if (other.away > away)
			return -1;
		else
			return 0;
	}

}
