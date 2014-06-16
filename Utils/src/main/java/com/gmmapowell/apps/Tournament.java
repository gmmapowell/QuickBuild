package com.gmmapowell.apps;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class Tournament {
	public static class Table implements Comparable<Table> {
		int[] pts = new int[4];
		int sortValue = -1;
		StringBuilder sb = new StringBuilder();
		
		public Table() {
		}

		public Table(Table prev) {
			for (int i=0;i<4;i++)
				pts[i] = prev.pts[i];
			sb.append(prev.sb.toString());
		}

		public Table home(int[] match) {
			Table ret = new Table(this);
			ret.pts[match[0]] += 3;
			ret.sb.append(":"+match[0] + ">" + match[1]);
			return ret;
		}

		public Table draw(int[] match) {
			Table ret = new Table(this);
			ret.pts[match[0]] += 1;
			ret.pts[match[1]] += 1;
			ret.sb.append(":"+match[0] + "-" + match[1]);
			return ret;
		}

		public Table loss(int[] match) {
			Table ret = new Table(this);
			ret.pts[match[1]] += 3;
			ret.sb.append(":"+match[0] + "<" + match[1]);
			return ret;
		}

		@Override
		public int compareTo(Table other) {
			if (sortValue == -1 || other.sortValue == -1)
				throw new UtilException("sort() not called");
			
			// We really want to sort on who comes third
			if (sortValue < other.sortValue)
				return -1;
			else if (sortValue > other.sortValue)
				return 1;

			// failing that, sort by highest-to-lowest
			for (int i=3;i>=0;i--) {
				if (pts[i] > other.pts[i])
					return -1;
				else if (pts[i] < other.pts[i])
					return 1;
			}
			
			// finally, if the pts are the same, sort on the way we got here
			return sb.toString().compareTo(other.sb.toString());
		}

		public Table sort() {
			if (sortValue != -1)
				throw new UtilException("sort() called multiple times");
			Arrays.sort(pts);
			sortValue = pts[1];
			return this;
		}

		@Override
		public String toString() {
			return CollectionUtils.listOf(pts[0], pts[1], pts[2], pts[3]).toString();
		}
	}

	private static int[][] matches = new int[][] { {0,1}, {2,3}, {0,2}, {1,3}, {0,3}, {1,2} };

	public static void main(String[] args) {
		Set<Table> result = new TreeSet<Table>();
		doit(result, new Table(), 0);
		System.out.println("total = " + result.size());
		for (Table t : result)
			System.out.println(t.toString() + t.sb);
	}

	private static void doit(Set<Table> result, Table curr, int i) {
		if (i == 6) {
//			System.out.println(curr.toString()+curr.sb);
			result.add(curr.sort());
			return;
		}
		doit(result, curr.home(matches[i]), i+1);
		doit(result, curr.draw(matches[i]), i+1);
		doit(result, curr.loss(matches[i]), i+1);
	}
}
