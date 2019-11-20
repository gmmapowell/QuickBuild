package com.gmmapowell.sync;

import java.util.Date;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.utils.SyncUtils;

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
	private Object value;
	private RuntimeException failure;

	public void release() {
		synchronized (this) {
			ready = true;
			this.notifyAll();
		}
	}
	
	public void release(Object value) {
		if (ready)
			return;
		this.value = value;
		release();
	}

	public void fail(Exception ex) {
		if (ready)
			return;
		this.failure = UtilException.wrap(ex);
		release();
	}
	
	public void hold() {
		while (true) {
			if (ready) {
				if (failure != null)
					throw failure;
				return;
			}
			SyncUtils.waitFor(this, 0);
		}
	}
	
	public boolean hold(int mstime) {
		if (mstime == 0) {
			hold();
			return true;
		}
			
		Date d = new Date(new Date().getTime() + mstime);
		while (true) {
			if (ready) {
				if (failure != null)
					throw failure;
				return true;
			}
			if (new Date().after(d))
				return false;
			SyncUtils.waitUntil(this, d);
		}
	}
	
	public Object getValue() {
		if (!ready)
			throw new UtilException("Cannot get value before ready");
		return value;
	}
}
