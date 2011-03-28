package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class AndroidSDKCommand extends NoChildCommand implements ConfigApplyCommand {
	private String sdk;
	private String platform;
	
	public AndroidSDKCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "sdk", "SDK directory"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "platform", "platform release dir")
		);
	}

	@Override
	public void applyTo(Config config) {
		config.setAndroidSDK(sdk, platform);
	}

}
