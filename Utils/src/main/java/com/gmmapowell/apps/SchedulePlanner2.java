package com.gmmapowell.apps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

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
		
//		int k = n;
		for (int k = 1;k<26; k++)
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
		List<Fixture> broken;
		while (true) {
			while (true) {
				// The simplest thing is just trying to slot fixtures in where we can
				broken = new ArrayList<Fixture>();
				Fixture next;
				while ((next = fixtures.choose()) != null) {
					if (!place(ret, next))
						broken.add(next);
				}
				if (broken.isEmpty())
					return ret;
				
//				System.out.println(ret);
//				System.out.println(broken);
		
				// When that fails, try to move the fixtures around and see what happens ...
				// This should have the effect of making the "left hand side" more complete
				boolean didSomething = tryShufflingIncompletes(ret, broken);
				
				if (broken.isEmpty())
					return ret;
				
//				System.out.println(ret);
//				System.out.println(broken);
				fixtures = new FixtureList(broken);
				
				if (!didSomething)
					break;
			}
			
			// now be more assertive
			// find an item from the broken list which can forcibly displace something somewhere so that
			// it can take its place.
			// The something must have one but not both of the same teams
			// The somewhere must be an incomplete row
			
			if (!tryForcibleMoves(ret, broken))
				break;
		}
		// Finally, just try random persuasion
		// TODO: I think a breadth-first search for possible chains of rearrangement would have to be more efficient
		int cnt = 10000000;
		loop:
		for (int i=0;i<cnt;i++) {
			if (broken.isEmpty()) {
				System.out.println("Finished after " + i + " iterations");
				return ret;
			}
			Fixture f = broken.remove(0);
			if (place(ret, f)) {
//				System.out.println("Placed " + f);
				continue;
			}
			// Look for volunteers
			for (Round r : ret.rounds) {
				Fixture h = r.fixtureFor(f.home);
				Fixture a = r.fixtureFor(f.away);
				if (h != null && a == null && place(ret, h)) {
					r.removeFixture(h);
					r.add(f);
					continue loop;
				} else if (h == null && a != null && place(ret, a)) {
					r.removeFixture(a);
					r.add(f);
					continue loop;
				}
			}
			{
				// OK, hack it
				Round r = ret.rounds.get(random.nextInt(ret.rounds.size()));
				Fixture h = r.fixtureFor(f.home);
				Fixture a = r.fixtureFor(f.away);
				if (h != null) {
					r.removeFixture(h);
					broken.add(h);
				}
				if (a != null && a != h) {
					r.removeFixture(a);
					broken.add(a);
				}
				r.add(f);
			}
//			System.out.println(ret);
//			System.out.println(broken);
		}
		throw new UtilException("There were " + broken.size() + " fixtures that could not be placed after " + cnt + " iterations");
	}

	private boolean tryShufflingIncompletes(Schedule ret, List<Fixture> broken) {
		boolean didSomething = false;
		
		List<Round> incomplete = figureIncompletes(ret);
		
		// For each round, try and find its missing fixture somewhere else
		for (Round r : incomplete) {
			List<String> toPlace = r.teamsMissing();
//			System.out.println(r.name + toPlace);
			for (Round s : incomplete) {
				if (r == s)
					continue;
				List<String> sMissing = s.teamsMissing();
				// Don't steal from someone in better shape than us
				if (sMissing.size() < toPlace.size())
					continue;
				for (String[] p : allPairs(toPlace)) {
					Fixture f;
					if ((f = s.hasFixture(p[0], p[1])) != null) {
						s.removeFixture(f);
						r.add(f);
						toPlace.remove(p[0]);
						toPlace.remove(p[1]);
						didSomething = true;
					}
				}
			}
		}
		
		return didSomething;
	}

	private boolean tryForcibleMoves(Schedule ret, List<Fixture> broken) {
		List<Round> incomplete = figureIncompletes(ret);

		// The moment we do anything, return true
		for (Fixture f : broken) {
			for (Round r : incomplete) {
				List<String> missing = r.teamsMissing();
				Fixture g = null;
				if (missing.contains(f.home) && !missing.contains(f.away))
					g = r.fixtureFor(f.home);
				else if (missing.contains(f.away) && !missing.contains(f.home))
					g = r.fixtureFor(f.away);
				if (g == null)
					continue;
				// OK, it has one but not the other ... would fixture g equally happy elsewhere?
				for (Round s : incomplete) {
					if (s == r)
						continue;
					if (!s.hasTeam(g.home) && !s.hasTeam(g.away)) {
						s.add(g);
						r.removeFixture(g);
						r.add(f);
						return true;
					}
				}
			}
		}
		
		// If we get here, we didn't manage to find anything to do
		return false;
	}

	private List<Round> figureIncompletes(Schedule ret) {
		// Find all the incomplete rounds
		List<Round> incomplete = new ArrayList<Round>();
		for (Round r : ret.rounds) {
			List<String> toPlace = r.teamsMissing();
			if (toPlace.isEmpty())
				continue;
			else
				incomplete.add(r);
		}
		return incomplete;
	}
	
	private List<String[]> allPairs(List<String> toPlace) {
		List<String[]> ret = new ArrayList<String[]>();
		for (int i=0;i<toPlace.size();i++)
			for (int j=i+1;j<toPlace.size();j++)
				ret.add(new String[] { toPlace.get(i), toPlace.get(j) });
		return ret;
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
				rounds.add(new Round(i+1));
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
			for (int i=0;i<(nteams+1)/2;i++) {
				for (Round r : rounds)
					r.dump(pp, i);
				pp.requireNewline();
			}
		}

		private void dumpTitles(PrettyPrinter pp) {
			for (Round r : rounds)
				pp.append(Justification.LEFT.format(r.name, 5));
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

		public FixtureList(List<Fixture> broken) {
			this.fixtures = broken;
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
		private final String name;
		
		public Round(int w) {
			name = "Wk" + w;
		}

		public void add(Fixture next) {
			fixtures.add(next);
		}
		
		public void removeFixture(Fixture f) {
			if (!fixtures.remove(f))
				throw new UtilException(this + " did not have " + f);
		}
		
		public List<String> teamsMissing() {
			List<String> ret = new ArrayList<String>(teams);
			for (Fixture f : fixtures) {
				ret.remove(f.home);
				ret.remove(f.away);
			}
			return ret;
		}

		public boolean hasTeam(String team) {
			for (Fixture f : fixtures)
				if (f.home.equals(team) || f.away.equals(team))
					return true;
			return false;
		}

		public Fixture hasFixture(String t1, String t2) {
			for (Fixture f : fixtures)
				if (f.home.equals(t1) && f.away.equals(t2))
					return f;
				else if (f.home.equals(t2) && f.away.equals(t1))
					return f;
			return null;
		}

		public Fixture fixtureFor(String t) {
			for (Fixture f : fixtures)
				if (f.home.equals(t) || f.away.equals(t))
					return f;
			return null;
		}

		public void dump(PrettyPrinter pp, int i) {
			String show;
			if (i >= fixtures.size())
				show = "";
			else
				show = CollectionUtils.nth(fixtures, i).toString();
			pp.append(Justification.LEFT.format(show, 5));
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
