package com.gmmapowell.adt.swt;

import com.gmmapowell.adt.ADTLayout;

public class SWTADTLayout implements ADTLayout {

	private final String name;

	public SWTADTLayout(String name) {
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
