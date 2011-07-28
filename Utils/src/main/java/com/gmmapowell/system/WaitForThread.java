package com.gmmapowell.system;

public class WaitForThread extends Thread{

	private final RunProcess rp;

	public WaitForThread(RunProcess rp) {
		this.rp = rp;
	}

	@Override
	public void run() {
		while (true)
		{
			try
			{
				rp.waitForEnd();
				if (rp.isFinished())
					return;
			}
			catch (InterruptedException ex)
			{
				// who cares
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
