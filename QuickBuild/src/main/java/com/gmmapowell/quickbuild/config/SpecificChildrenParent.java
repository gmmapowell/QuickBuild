package com.gmmapowell.quickbuild.config;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.Parent;

public abstract class SpecificChildrenParent<T> implements Parent<T> {
	private final Class<? extends T>[] clzs;

	public SpecificChildrenParent(Class<? extends T>... clzs) {
		this.clzs = clzs;
	}

	public void checkChild(T obj) {
		if (!isInList(obj.getClass()))
			throw new UtilException("You cannot add an object of class '" + obj.getClass() + "' to '" + this);
	}

	private boolean isInList(Class<?> oclz) {
		for (Class<? extends T> clz : clzs)
			if (clz.isAssignableFrom(oclz))
			{
				return true;
			}
		return false;
	}

}
