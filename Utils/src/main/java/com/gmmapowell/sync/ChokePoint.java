package com.gmmapowell.sync;

/** A choke point is a synchronization object which holds up any number of "waiting" threads
 * until a "master" thread releases the choke.
 *
 * <p>
 * &copy; 2013 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ChokePoint {
	private boolean ready;

	public void release() {
		synchronized (this) {
			ready = true;
			this.notifyAll();
		}
	}
	
	public void hold() {
		while (true) {
			if (ready) return;
			SyncUtils.waitFor(this, 0);
		}
	}
}
