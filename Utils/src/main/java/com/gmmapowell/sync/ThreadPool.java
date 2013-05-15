package com.gmmapowell.sync;

import java.util.ArrayList;
import java.util.List;


public class ThreadPool {
	public class PooledThread extends Thread {
		private ChokePoint myChoke;

		public PooledThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			while (true) {
				try {
					myChoke = new ChokePoint();
					synchronized(pool) {
						if (pool.isEmpty())
							pool.notify();
						pool.add(this);
					}
					myChoke.hold();
					Runnable r = (Runnable) myChoke.getValue();
					myChoke = null;
					r.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	private List<PooledThread> pool = new ArrayList<PooledThread>(); 

	public ThreadPool(int cnt) {
		synchronized (this) {
			for (int i=0;i<cnt;i++) {
				PooledThread thr = new PooledThread("Pool " + i);
				thr.start();
			}
		}
	}
	
	public void run(Runnable r) {
		synchronized (pool) {
			while (pool.isEmpty())
				SyncUtils.waitFor(pool, 0);
			pool.remove(0).myChoke.release(r);
		}
	}
}
