package com.gmmapowell.utils;

import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;

public class Hollerith {

	private final HollerithFormat fmt;
	private final Map<String, String> fields = new HashMap<String, String>();

	public Hollerith(HollerithFormat fmt) {
		this.fmt = fmt;
	}

	public String format() {
		return fmt.assemble(fields);
	}

	public void set(String field, String value) {
		if (!fmt.hasField(field))
			throw new UtilException("Cannot bind to hollerith field " + field + " because it is not in " + fmt);
		fields.put(field, value);
	}

}
