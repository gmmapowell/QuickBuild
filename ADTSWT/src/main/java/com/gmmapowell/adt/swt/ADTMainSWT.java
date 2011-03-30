package com.gmmapowell.adt.swt;

import com.gmmapowell.adt.ScreenRotation;
import com.gmmapowell.adt.avm.ADTInitialize;

public class ADTMainSWT {
	
	public static void main(String[] args)
	{
		// TODO: we should pull in a config file and have a lot of stuff configure itself
		int width = 320;
		int height = 480;
		ScreenRotation rotation = ScreenRotation.UPRIGHT;
		
		// Create the window as soon as possible
		SWTDisplay display = new SWTDisplay(width, height, rotation);
		
		// Delegate setting up of virtual machine to another thread
		new Thread(new ADTInitialize(display.getCanvas(), args)).start();
		
		// Drop into the loop
		display.loop();
	}
}
