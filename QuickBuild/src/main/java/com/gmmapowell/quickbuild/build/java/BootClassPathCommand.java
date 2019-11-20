package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class BootClassPathCommand extends NoChildCommand implements ConfigApplyCommand {
	private String config;
	private File bootClass;
	
	public BootClassPathCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "config", "boot configuration"));
	}

	@Override
	public void applyTo(Config config) {
		if (this.config.equals("android"))
		{
			bootClass = config.getAndroidContext().getPlatformJar();
		}
		else
			throw new UtilException("Unrecognized boot option: " + bootClass);
	}

	public File getFile() {
		return bootClass;
	}
	
	@Override
	public String toString() {
		return "boot " + config;
	}

}
