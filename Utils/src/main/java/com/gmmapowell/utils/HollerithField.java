package com.gmmapowell.utils;

import java.util.Map;

public class HollerithField {
	private final String field;
	private Justification justify = Justification.LEFT;
	private int width = -1;
	private String heading;

	public HollerithField(String field) {
		this.field = field;
		this.heading = field;
	}

	public HollerithField setWidth(int i) {
		width  = i;
		return this;
	}

	public HollerithField setJustification(Justification j) {
		justify = j;
		return this;
	}

	@Override
	public String toString() {
		return field;
	}

	public String apply(Map<String, String> values) {
		String val = "";
		if (field != null && values.containsKey(field))
			val = values.get(field);
		return justify.format(val, width);
	}

	public String getHeading() {
		return heading;
	}
}
