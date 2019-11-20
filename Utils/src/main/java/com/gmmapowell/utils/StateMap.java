package com.gmmapowell.utils;

import java.util.HashMap;
import java.util.Map;

import org.zinutils.exceptions.UtilException;

public class StateMap<K, V> {
	private Map<K, V> map = new HashMap<K, V>();
	
	public V get(K k) {
		if (!containsKey(k))
			throw new UtilException("There is no key " + k);
		return map.get(k);
	}
	
	public void save(K k, V v)
	{
		map.put(k, v);
	}
	
	public void op(K k, V v, FuncR1<V, V> func)
	{
		if (map.containsKey(k))
			map.put(k, func.apply(map.get(k)));
		else 
			map.put(k, v);
	}

	public boolean containsKey(K k) {
		return map.containsKey(k);
	}

	public V require(K k, Class<?> cls) {
		try {
			if (containsKey(k))
				return map.get(k);
			@SuppressWarnings("unchecked")
			V newv = (V) cls.newInstance();
			save(k, newv);
			return newv;
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}
}
