package com.gmmapowell.extrep;

import java.util.ArrayList;
import java.util.List;

public abstract class ERSoup {
	protected List<ERObject> children = new ArrayList<ERObject>();

	public abstract ERObject addObject(String tag);
}
