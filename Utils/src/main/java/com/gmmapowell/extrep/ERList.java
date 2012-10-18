package com.gmmapowell.extrep;

import java.util.ArrayList;
import java.util.List;

public abstract class ERList {
	protected List<Object> children = new ArrayList<Object>();
	
	public ERObject addObject(String tag) {
		ERObject ret = newObject(tag);
		children.add(ret);
		return ret;
	}
	
	public void addValue(String s) {
		children.add(s);
	}
	
	protected abstract ERObject newObject(String tag);
}
