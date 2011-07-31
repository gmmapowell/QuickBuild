package com.gmmapowell.apps;

import com.gmmapowell.utils.StringUtil;

public class Playbook {
	private Integer[][] weekteam;
	private Integer[][] fixture;
	
	public Playbook(int nt) {
		weekteam = new Integer[nt-1][nt];
		fixture = new Integer[nt][nt];
	}

	public boolean playedWeek(int w, int i) {
		return weekteam[w][i] != null;
	}

	public boolean played(int i, int j) {
		return fixture[i][j] != null;
	}

	public void record(int w, int i, int j) {
//		System.out.println(team(i) + "v" + team(j));
		weekteam[w][i] = j;
		weekteam[w][j] = i;
		fixture[i][j] = w;
	}

	public void delete(int w, int i, int j) {
		weekteam[w][i] = null;
		weekteam[w][j] = null;
		fixture[i][j] = null;
	}

	public Schedule schedule() {
		// Add all the fixtures in a sane fashion
		Schedule ret = new Schedule(weekteam.length, weekteam[0].length/2);
		int[] idx = new int[weekteam.length];
		for (int k = 0;k<idx.length;k++)
			idx[k] = -1;
		for (int fx=0;fx<weekteam[0].length/2;fx++)
		{
			for (int w=0;w<weekteam.length;w++)
			{
				do {
					idx[w]++;
				} while (idx[w] > weekteam[w][idx[w]]);
				ret.add(w, fx, idx[w], weekteam[w][idx[w]]);
			}
		}
		
		ret.muddle();
		return ret;
	}
	
	public void print() {
		int[] idx = new int[weekteam.length];
		for (int k = 0;k<idx.length;k++)
			idx[k] = -1;
		System.out.print("       ");
		for (int w = 0;w<weekteam.length;w++)
			System.out.print(" Wk"+StringUtil.digits(w+1, 2));
		System.out.println();
		for (int fx=0;fx<weekteam[0].length/2;fx++)
		{
			System.out.print("Fix " + StringUtil.digits(fx+1,2) + ":");
			for (int w=0;w<weekteam.length;w++)
			{
				do {
					idx[w]++;
				} while (idx[w] > weekteam[w][idx[w]]);
				System.out.print("  " +team(idx[w]) + "v" + team(weekteam[w][idx[w]]));
			}
			System.out.println();
		}
	}

	static char team(Integer team) {
		if (team >= 26)
			return (char)('0'+team-26);
		return (char) ('A'+team);
	}

}
