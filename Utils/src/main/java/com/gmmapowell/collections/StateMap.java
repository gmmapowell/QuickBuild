package com.gmmapowell.collections;

import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.lambda.Func1R;
import com.gmmapowell.utils.ArgumentDefinition;

public class StateMap<K, V> {
	private Map<K, V> map = new HashMap<K, V>();
	
	public void save(K k, V v)
	{
		map.put(k, v);
	}
	
	public void op(K k, V v, Func1R<V, V> func)
	{
		if (map.containsKey(k))
			map.put(k, func.apply(map.get(k)));
		else 
			map.put(k, v);
	}

	public boolean containsKey(ArgumentDefinition ad) {
		return map.containsKey(ad);
	}
}
