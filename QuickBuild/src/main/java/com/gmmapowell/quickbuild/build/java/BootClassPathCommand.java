package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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

}
