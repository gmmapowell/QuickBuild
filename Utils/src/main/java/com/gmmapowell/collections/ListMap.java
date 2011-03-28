package com.gmmapowell.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;

public class ListMap<K, V> implements Iterable<K> {
	private Map<K, List<V>> map = new HashMap<K, List<V>>();
	
	public void add(K k, V v)
	{
		if (!map.containsKey(k))
			map.put(k, new ArrayList<V>());
		map.get(k).add(v);
	}

	@Override
	public Iterator<K> iterator() {
		return map.keySet().iterator();
	}
	
	public List<V> get(K k)
	{
		if (!map.containsKey(k))
			throw new UtilException("There is no key '" + k + "' in " + this);
		return map.get(k);
	}

	public boolean contains(K key) {
		return map.containsKey(key);
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public Collection<V> values() {
		Set<V> ret = new HashSet<V>();
		for (List<V> vs : map.values())
			ret.addAll(vs);
		return ret;
	}
}
