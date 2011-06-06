package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class Notification {
	private final Class<? extends BuildResource> cls;
	private final Nature nature;

	public Notification(Class<? extends BuildResource> cls, Nature nature) {
		this.cls = cls;
		this.nature = nature;
	}

	public void dispatch(BuildResource br)
	{
		if (cls.isAssignableFrom(br.getClass()))
			nature.resourceAvailable(br);
	}
}