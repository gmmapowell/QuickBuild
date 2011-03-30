package com.gmmapowell.adt;

public class ADTIntent {
	private final ADTContext context;
	private final Class<? extends ADTActivity> activity;

	public ADTIntent(ADTContext context, Class<? extends ADTActivity> activity) {
		this.context = context;
		this.activity = activity;
	}

	public boolean isAbsolute() {
		return activity != null;
	}

	public Class<? extends ADTActivity> absoluteClass() {
		return activity;
	}

	public ADTContext getContext() {
		return context;
	}

}
