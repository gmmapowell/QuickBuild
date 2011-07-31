package com.gmmapowell.apps;

import java.util.Arrays;
import java.util.Random;

import com.gmmapowell.utils.StringUtil;

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
				System.out.print("  " +Playbook.team(f.home) + "v" + Playbook.team(f.away));
			}
			System.out.println();
		}
	}
}
