package com.gmmapowell.adt.avm;

import com.gmmapowell.adt.ADTContext;
import com.gmmapowell.adt.swt.AndroidScreen;
import com.gmmapowell.adt.swt.SWTADTLayout;

public class ADTSWTContext implements ADTContext {

	private final Repository repository;
	private final AndroidScreen androidScreen;

	public ADTSWTContext(Repository repository, AndroidScreen androidScreen)
	{
		this.repository = repository;
		this.androidScreen = androidScreen;
	}
	
	@Override
	public void setContentView(int resId) {
		SWTADTLayout layout = (SWTADTLayout) repository.getLayout(resId);
		androidScreen.setLayout(layout);
	}

}
