package com.gmmapowell.sync;

import java.util.Date;

public class SyncUtils {

	public static boolean waitFor(Object waitOn, int ms) {
		Date d = new Date();
		d = new Date(d.getTime() + ms);
		synchronized (waitOn) {
			while (ms == 0 || new Date().before(d)) {
				try {
					waitOn.wait(ms);
					return true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public static void sleep(int ms) {
		Date end = new Date();
		end = new Date(end.getTime() + ms);
		Date curr;
		while ((curr = new Date()).before(end)) {
			try {
				Thread.sleep(end.getTime() - curr.getTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void join(Thread thr) {
		while (thr.isAlive()) {
			try {
				thr.join();
			} catch (InterruptedException ex) {
				// no worries
			}
		}
	}

	public static ChokePoint chokePoint() {
		return new ChokePoint();
	}

}
