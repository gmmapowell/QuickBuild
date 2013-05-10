package com.gmmapowell.apps;

import java.util.Arrays;
import java.util.Random;

import com.gmmapowell.utils.StringUtil;

public class SchedulePlanner {

	public static void main(String[] args) {
		if (args.length != 1)
		{
			System.out.println("Usage: plan #teams");
			return;
		}
		int k = Integer.parseInt(args[0]);
		if (k < 2 || k % 2 == 1)
		{
			System.out.println("The number of teams must be a multiple of 2");
			return;
		}
//		for (int k = 2;k<30; k+=2)
		{
			SchedulePlanner plan = new SchedulePlanner(k);
			int found = plan.findFixtures(0, 0);
			System.out.println("Found " + found + " solutions after " + plan.backtrack + " backtracks");
			plan.playbook.schedule().print();
		}
	}

	private int nteams;
	private Playbook playbook;
	private int backtrack;

	public SchedulePlanner(int nt) {
		nteams = nt;
		playbook = new Playbook(nt);
	}

	// Come up with a plan for week w, fixture f
	private int findFixtures(int w, int f) {
//		System.out.println("w="+w+" f="+f);
		if (w == nteams-1)
		{
			playbook.print();
			return 1;
		}
		else if (f == nteams/2)
		{
			return findFixtures(w+1, 0);
		}
		int played = 0;
		// try and find the first team that can play
		for (int i=0;i<nteams;i++)
		{
			if (playbook.playedWeek(w, i))
				continue;
			for (int j=i+1;j<nteams;j++)
			{
				if (playbook.playedWeek(w, j))
					continue;
				if (playbook.played(i, j))
					continue;
				playbook.record(w, i, j);
				played += findFixtures(w, f+1);
				if (played > 0)
					return played;
				playbook.delete(w, i, j);
				backtrack++;
			}
		}
		if (played == 0)
		{
			playbook.print();
			System.out.println("Backtracking with no success from w = " + w + " f = " + f);
			return 0;
		}
		return played;
	}
	
	static char team(Integer team) {
		if (team >= 26)
			return (char)('0'+team-26);
		return (char) ('A'+team);
	}

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
	public class Playbook {
		private final int nt;
		private int nweeks;
		private int nfix;
		private Integer[][] weekteam;
		private Integer[][] fixture;
		
		public Playbook(int nt) {
			this.nt = nt;
			this.nweeks = nt-1;
			this.nfix = nt/2;
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
//			System.out.println(team(i) + "v" + team(j));
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
			int[] idx = new int[nweeks];
			for (int k = 0;k<nweeks;k++)
				idx[k] = -1;
			System.out.print("       ");
			for (int w = 0;w<nweeks;w++)
				System.out.print(" Wk"+StringUtil.digits(w+1, 2));
			System.out.println();
			for (int fx=0;fx<nfix;fx++)
			{
				System.out.print("Fix " + StringUtil.digits(fx+1,2) + ":");
				for (int w=0;w<nweeks;w++)
				{
					do {
						idx[w]++;
					} while (idx[w] < nt && (weekteam[w][idx[w]] == null || idx[w] > weekteam[w][idx[w]]));
					if (idx[w] < nt)
						System.out.print("  " +team(idx[w]) + "v" + team(weekteam[w][idx[w]]));
					else
						System.out.print(" VOID");
				}
				System.out.println();
			}
		}

	}

	public class Schedule {
		private Fixture[][] sched;
		private final int nweeks;
		private final int nfix;

		public Schedule(int nweeks, int nfix) {
			this.nweeks = nweeks;
			this.nfix = nfix;
			sched = new Fixture[nweeks*2][nfix];
		}

		public void add(int week, int fx, int home, int away) {
			sched[week][fx] = new Fixture(home, away);
			sched[nweeks+week][fx] = new Fixture(away, home);
		}

		public Schedule muddle() {
			Random r = new Random();
			for (int i=0;i<nweeks*nfix;i++)
				shuffleHA(r);
			for (int i=0;i<nweeks*10;i++)
				shuffleWeek(r);
			for (int i=0;i<nweeks*2;i++)
				Arrays.sort(sched[i]);
			return this;
		}

		private void shuffleWeek(Random r) {
			int from = r.nextInt(2*nweeks);
			int to = r.nextInt(2*nweeks);
			if (from != to)
			{
				Fixture[] tmp = sched[from];
				sched[from] = sched[to];
				sched[to] = tmp;
			}
		}

		private void shuffleHA(Random r) {
			int w = r.nextInt(nweeks);
			int f = r.nextInt(nfix);
			sched[w][f].reverse();
			sched[w+nweeks][f].reverse();
		}
		
		
		public void print() {
			System.out.print("       ");
			for (int w = 0;w<2*nweeks;w++)
				System.out.print(" Wk"+StringUtil.digits(w+1, 2));
			System.out.println();
			for (int fx=0;fx<nfix;fx++)
			{
				System.out.print("Fix " + StringUtil.digits(fx+1,2) + ":");
				for (int w=0;w<2*nweeks;w++)
				{
					Fixture f = sched[w][fx];
					System.out.print("  " +team(f.home) + "v" + team(f.away));
				}
				System.out.println();
			}
		}
	}

	
}