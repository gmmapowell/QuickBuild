package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class StrategemResource extends SolidResource {

	public StrategemResource(Strategem builtBy) {
		super(builtBy, builtBy.rootDirectory());
	}

	@Override
	public String compareAs() {
		return "Strategem["+relative+"]";
	}

}
