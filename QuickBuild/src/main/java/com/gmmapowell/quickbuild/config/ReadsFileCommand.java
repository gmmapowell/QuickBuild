package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import org.zinutils.utils.FileUtils;

public class ReadsFileCommand extends NoChildCommand implements ConfigApplyCommand {
	private String file;
	private boolean absolute;
	private File path;
	
	public ReadsFileCommand(TokenizedLine toks)
	{
		toks.process(this, 
			new ArgumentDefinition("--absolute", Cardinality.OPTION, "absolute", "the path is absolute"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "file", "file"));
	}

	@Override
	public void applyTo(Config config) {
		if (absolute)
			path = new File(file);
		else
			path = FileUtils.relativePath(file);
	}
	
	@Override
	public String toString() {
		return "ReadsFile["+getPath()+"]";
	}

	public File getPath() {
		return path;
	}
}
