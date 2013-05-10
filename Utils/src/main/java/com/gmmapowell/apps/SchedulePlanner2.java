package com.gmmapowell.apps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.apps.SchedulePlanner2.Round;
import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.Justification;
import com.gmmapowell.utils.PrettyPrinter;

public class SchedulePlanner2 {

	public static void main(String[] args) {
		if (args.length != 2)
		{
			System.out.println("Usage: plan #teams #bothWays");
			return;
		}
		int n = Integer.parseInt(args[0]);
		if (n < 1)
		{
			System.out.println("There must be at least one team");
			return;
		}
		boolean bothWays = Boolean.parseBoolean(args[1]);
		
		int k = n;
//		for (int k = 1;k<30; k+=2)
		{
			SchedulePlanner2 planner = new SchedulePlanner2(k, bothWays);
			Schedule s = planner.findFixtures();
			System.out.println(s);
		}
	}

	private int nteams;
	private final boolean bothWays;
	private List<String> teams;
	private final Random random = new Random();

	public SchedulePlanner2(int nt, boolean bothWays) {
		this.nteams = nt;
		this.bothWays = bothWays;
		this.teams = createTeams(nt);
	}

	private List<String> createTeams(int nt) {
		List<String> ret = new ArrayList<String>();
		for (int i=0;i<nt;i++) {
			ret.add(new String(new char[]{(char)('A'+i)}));
		}
		if (nt%2 == 1)
			ret.add("!");
		return ret;
	}

	// Come up with a plan for week w, fixture f
	private Schedule findFixtures() {
		Schedule ret = new Schedule(nteams, bothWays);
		FixtureList fixtures = new FixtureList(teams, bothWays);
		List<Fixture> broken = new ArrayList<Fixture>();
		Fixture next;
		while ((next = fixtures.choose()) != null) {
			if (!place(ret, next))
				broken.add(next);
		}
		if (broken.isEmpty())
			return ret;
		
		System.out.println(ret);
		System.out.println(broken);
		throw new UtilException("There were " + broken.size() + " fixtures that could not be placed");
	}

	private boolean place(Schedule ret, Fixture next) {
		Set<Round> forHome = ret.availableFor(next.home);
		Set<Round> forAway = ret.availableFor(next.away);
		forHome.retainAll(forAway);
		if (forHome.isEmpty())
			return false;
		Round use = CollectionUtils.nth(forHome, random.nextInt(forHome.size()));
		use.add(next);
		return true;
	}

	public class Schedule {
		final List<Round> rounds;
		
		public Schedule(int nteams, boolean bothWays) {
			int nrounds;
			if (bothWays) {
				if (nteams%2 == 1)
					nrounds = 2*nteams;
				else
					nrounds = 2*nteams - 2;
			} else {
				if (nteams%2 == 1)
					nrounds = nteams;
				else
					nrounds = nteams-1;
			}
				
			rounds = new ArrayList<Round>(nrounds);
			for (int i=0;i<nrounds;i++)
				rounds.add(new Round());
		}

		public Set<Round> availableFor(String t) {
			Set<Round> ret = new HashSet<Round>();
			for (Round r : rounds)
				if (!r.hasTeam(t))
					ret.add(r);
			return ret;
		}

		private void dump(PrettyPrinter pp) {
			dumpTitles(pp);
			for (int i=0;i<nteams;i++) {
				for (Round r : rounds)
					r.dump(pp, i);
				pp.requireNewline();
			}
		}

		private void dumpTitles(PrettyPrinter pp) {
			int w = 1;
			for (@SuppressWarnings("unused") Round r : rounds)
				pp.append(Justification.LEFT.format("Wk"+(w++), 5));
			pp.requireNewline();
		}

		@Override
		public String toString() {
			PrettyPrinter pp = new PrettyPrinter();
			dump(pp);
			return pp.toString();
		}
	}

	public class FixtureList {

		private List<Fixture> fixtures;

		public FixtureList(List<String> teams, boolean bothWays) {
			fixtures = new ArrayList<Fixture>();
			if (bothWays) {
				for (String h : teams)
					for (String a : teams)
						if (!h.equals(a))
							fixtures.add(new Fixture(h,a));
			} else {
				for (int i=0;i<teams.size();i++)
					for (int j=i+1;j<teams.size();j++)
						fixtures.add(new Fixture(teams.get(i), teams.get(j)));
			}
		}

		public Fixture choose() {
			if (fixtures.isEmpty())
				return null;
			return fixtures.remove(random.nextInt(fixtures.size()));
		}

	}

	public class Fixture implements Comparable<Fixture> {
		private final String home;
		private final String away;

		public Fixture(String home, String away) {
			this.home = home;
			this.away = away;
		}
		
		@Override
		public String toString() {
			if (home.equals("!"))
				return "[" + away + "]";
			else if (away.equals("!"))
				return "[" + home + "]";
			else
				return home + "-" + away;
		}

		@Override
		public int compareTo(Fixture other) {
			if (this == other)
				return 0;
			if (this.home.equals("!") || this.away.equals("!"))
				return 1;
			if (other.home.equals("!") || other.away.equals("!"))
				return -1;
			return this.home.compareTo(other.home);
		}
	}

	public class Round {
		private Set<Fixture> fixtures = new TreeSet<Fixture>();
		
		public void add(Fixture next) {
			fixtures.add(next);
		}
		
		public boolean hasTeam(String team) {
			for (Fixture f : fixtures)
				if (f.home.equals(team) || f.away.equals(team))
					return true;
			return false;
		}

		public void dump(PrettyPrinter pp, int i) {
			String show;
			if (i >= fixtures.size())
				show = "";
			else
				show = CollectionUtils.nth(fixtures, i).toString();
			pp.append(Justification.LEFT.format(show, 5));
		}

	}
}