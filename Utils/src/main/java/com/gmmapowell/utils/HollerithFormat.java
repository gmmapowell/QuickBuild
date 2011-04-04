package com.gmmapowell.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gmmapowell.exceptions.UtilException;

public class HollerithFormat {
	private List<HollerithField> order = new ArrayList<HollerithField>();
	private Map<String,HollerithField> fields = new HashMap<String, HollerithField>();

	public HollerithField addField(String field) {
		if (hasField(field))
			throw new UtilException("The field " + field + " cannot be defined twice");
		HollerithField f = new HollerithField(field);
		fields.put(field, f);
		order.add(f);
		return f;
	}

	public void addPadding(int i) {
		order.add(new HollerithField(null).setWidth(i));
	}

	public void titles(Hollerith hollerith) {
		for (Entry<String, HollerithField> i : fields.entrySet())
		{
			hollerith.set(i.getKey(), i.getValue().getHeading());
		}
		
	}

	public boolean hasField(String field) {
		return fields.containsKey(field);
	}

	
	@Override
	public String toString() {
		// I think what I really want is a functional composition stringbuilder
		StringBuilder ret = new StringBuilder();
		ret.append("HollerithFormat{");
		for (HollerithField e : order)
		{
			ret.append(e+",");
		}
		return ret.toString();
	}

	public String assemble(Map<String, String> values) {
		StringBuilder ret = new StringBuilder();
		for (HollerithField f : order)
			ret.append(f.apply(values));
		return ret.toString();
	}
}
