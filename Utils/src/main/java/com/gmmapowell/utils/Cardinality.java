package com.gmmapowell.utils;

public enum Cardinality {
	OPTION, REQUIRED, REQUIRED_ALLOW_FLAGS, ZERO_OR_MORE, ONE_OR_MORE, LIST;

	public boolean maxOfOne() {
		return this == OPTION || this == REQUIRED || this == REQUIRED_ALLOW_FLAGS;
	}

	public boolean isRequired() {
		return this == Cardinality.REQUIRED || this == Cardinality.ONE_OR_MORE || this == REQUIRED_ALLOW_FLAGS;
	}
}
