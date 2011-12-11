package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class ImageMagickLauncherIcon extends NoChildCommand implements ConfigApplyCommand {
	private String source;
	private final File file;
	
	public ImageMagickLauncherIcon(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "source", "source file"));
		file = FileUtils.relativePath(source);
	}

	@Override
	public void applyTo(Config config) {
	}

	public File getSource() {
		return file;
	}
	
	@Override
	public String toString() {
		return "Launcher["+source+"]";
	}


}
