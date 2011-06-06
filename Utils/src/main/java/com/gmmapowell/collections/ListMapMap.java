package com.gmmapowell.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.gmmapowell.exceptions.UtilException;

/**
 * A mapping from the cartesian product of two from types to a list of possible values.
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 * @param <F1>
 * @param <F2>
 * @param <T>
 */
public class ListMapMap<F1, F2, T> {
	private Map<F1, ListMap<F2, T>> map = new HashMap<F1, ListMap<F2, T>>();
	
	public void add(F1 f1, F2 f2, T v)
	{
		if (!map.containsKey(f1))
			map.put(f1, new ListMap<F2, T>());
		map.get(f1).add(f2, v);
	}

	public List<T> get(F1 f1, F2 f2)
	{
		if (!map.containsKey(f1))
			throw new UtilException("There is no key '" + f1 + "' in " + this);
		return map.get(f1).get(f2);
	}

	public boolean contains(F1 f1, F2 f2) {
		return map.containsKey(f1) && map.get(f1).contains(f2);
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

	public Collection<T> values() {
		Set<T> ret = new HashSet<T>();
		for (ListMap<F2, T> lm : map.values())
			ret.addAll(lm.values());
		return ret;
	}
}
