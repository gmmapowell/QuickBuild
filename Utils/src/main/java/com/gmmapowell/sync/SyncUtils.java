package com.gmmapowell.sync;

import java.util.Date;

public class SyncUtils {

	public static boolean waitFor(Object waitOn, int ms) {
		Date d = new Date();
		d = new Date(d.getTime() + ms);
		synchronized (waitOn) {
			while (new Date().before(d)) {
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

}
