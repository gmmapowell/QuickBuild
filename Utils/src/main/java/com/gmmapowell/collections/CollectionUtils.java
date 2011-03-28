package com.gmmapowell.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;

public class CollectionUtils {
	public static <T> List<T> setToList(Set<T> in, Comparator<T> ordering)
	{
		List<T> ret = new ArrayList<T>();
		ret.addAll(in);
		Collections.sort(ret, ordering);
		return ret;
	}
	
	public static <T> T any(Iterable<T> coll)
	{
		Iterator<T> it = coll.iterator();
		if (!it.hasNext())
			throw new UtilException("Any requires at least one element to function");
		return it.next();
	}
}