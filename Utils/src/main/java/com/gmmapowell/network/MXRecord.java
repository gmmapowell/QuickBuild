package com.gmmapowell.network;

public class MXRecord implements Comparable<MXRecord> {
	public final int level;
	public final String server;

	public MXRecord(int level, String server) {
		this.level = level;
		this.server = server;
	}

	@Override
	public int compareTo(MXRecord o) {
		if (level < o.level)
			return -1;
		else if (level == o.level)
			return 0;
		else
			return 1;
	}
	
	@Override
	public String toString() {
		return "MX["+level+":"+server+"]";
	}
}
