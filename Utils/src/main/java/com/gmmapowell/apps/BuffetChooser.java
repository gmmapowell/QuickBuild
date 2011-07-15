package com.gmmapowell.apps;

import java.util.Random;

public class BuffetChooser {
	public static void main(String[] argv)
	{
		Random r = new Random();
		for (int i=0;i<20;i++)
			System.out.println(new String[] { "Bally's", "Caesar's", "Paris" }[r.nextInt(3)]);
	}
}
