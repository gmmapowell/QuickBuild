package com.gmmapowell.utils;

public enum Cardinality {
	OPTION, REQUIRED, ZERO_OR_MORE, ONE_OR_MORE, LIST;

	public boolean maxOfOne() {
		return this == OPTION || this == REQUIRED;
	}

	public boolean isRequired() {
		return this == Cardinality.REQUIRED || this == Cardinality.ONE_OR_MORE;
	}
}
