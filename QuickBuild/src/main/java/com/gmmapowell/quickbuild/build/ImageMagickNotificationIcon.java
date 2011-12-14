package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class ImageMagickNotificationIcon extends NoChildCommand implements ConfigApplyCommand {
	private String source;
	private String called;
	private final File file;
	
	public ImageMagickNotificationIcon(TokenizedLine toks)
	{
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "source", "source file"),
				new ArgumentDefinition("*", Cardinality.OPTION, "called", "destination file"));
		file = FileUtils.relativePath(source);
	}

	@Override
	public void applyTo(Config config) {
	}

	public File getSource() {
		return file;
	}
	
	public String getCalled() {
		if (called != null)
			return called;
		return file.getName();
	}
	
	@Override
	public String toString() {
		return "Launcher["+source+"]";
	}


}
