package com.gmmapowell.quickbuild.build;

import java.io.File;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;

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
