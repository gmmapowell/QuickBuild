package com.gmmapowell.extrep;

import java.util.HashMap;
import java.util.Map;

public abstract class ERObject {
	protected final String tag;
	protected final Map<String, Object> attrs = new HashMap<String, Object>();
	protected final Map<String, Object> elements = new HashMap<String, Object>();
	protected ERSoup soup;
	
	protected ERObject(String tag) {
		this.tag = tag;
	}
	
	public void setAttr(String field, String text) {
		if (field == null)
			throw new ERException("Cannot set attribute field null");
		if (attrs.containsKey(field))
			throw new ERException("Cannot have duplicate attribute " + field);
		else if (elements.containsKey(field))
			throw new ERException("Cannot have attribute " + field + " with same name as element");
		if (text == null)
			throw new ERException("Cannot set attribute value to null");
		attrs.put(field, text);
	}

	public ERObject addObject(String field, String tag) {
		if (attrs.containsKey(field))
			throw new ERException("Cannot have duplicate element " + field);
		else if (elements.containsKey(field))
			throw new ERException("Cannot have element " + field + " with same name as attribute");
		
		ERObject ret = newObject(tag);
		elements.put(field, ret);
		return ret;
	}

	public ERList addList(String field) {
		if (attrs.containsKey(field))
			throw new ERException("Cannot have duplicate element " + field);
		else if (elements.containsKey(field))
			throw new ERException("Cannot have element " + field + " with same name as attribute");
		
		ERList ret = newList();
		elements.put(field, ret);
		return ret;
	}

	public ERSoup getSoup() {
		if (soup == null)
			soup = newSoup();
		return soup;
	}

	protected abstract ERObject newObject(String tag);
	protected abstract ERList newList();
	protected abstract ERSoup newSoup();
	public abstract void done();
}
