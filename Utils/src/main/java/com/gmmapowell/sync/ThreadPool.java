package com.gmmapowell.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.gmmapowell.utils.StringUtil;


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
					if (done)
						break;
					myChoke.hold();
					Object val = myChoke.getValue();
					if (val instanceof Exception)
						break;
					Runnable r = (Runnable) val;
					myChoke = null;
					r.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			closeLatch.countDown();
		}
	}

	private boolean done = false;
	private final List<PooledThread> pool = new ArrayList<PooledThread>();
	private CountDownLatch closeLatch;

	public ThreadPool(int cnt) {
		synchronized (pool) {
			closeLatch = new CountDownLatch(cnt);
			for (int i=0;i<cnt;i++) {
				PooledThread thr = new PooledThread("Pool " + StringUtil.digits(i, 2));
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

	public void destroy() {
		synchronized (pool) {
			done = true;
			for (PooledThread pt : pool)
				pt.myChoke.release(new InterruptedException());
		}
		while (true)
			try {
				closeLatch.await();
				break;
			} catch (InterruptedException e) {
			}
		// Now everything should be back in the pool ...
		synchronized (pool) {
			loop:
			for (PooledThread pt : pool)
				while (true) {
					try {
						pt.join();
						continue loop;
					} catch (InterruptedException e) {
					}
				}
		}
	}
}
