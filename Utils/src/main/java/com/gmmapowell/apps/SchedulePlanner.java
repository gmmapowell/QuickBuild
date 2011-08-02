package com.gmmapowell.apps;

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
}