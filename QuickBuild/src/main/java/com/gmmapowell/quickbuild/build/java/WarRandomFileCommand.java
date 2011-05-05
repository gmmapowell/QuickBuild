package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class WarRandomFileCommand extends NoChildCommand implements ConfigApplyCommand {
	private String projectName;
	private String relativePath;
	private String file;
	private String prepend;
	private PendingResource pend;
	
	public WarRandomFileCommand(TokenizedLine toks)
	{
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "project"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "relativePath", "path"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "file", "file"),
				new ArgumentDefinition("*", Cardinality.OPTION, "prepend", "prepend")
				);
		pend = new PendingResource(projectName);
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

	public PendingResource getPendingResource() {
		return pend;
	}

	public File getFrom(BuildContext cxt) {
		BuildResource br = cxt.getPendingResource(pend);
		File root = br.getPath();
		return new File(new File(root, relativePath), file);
	}

	public File getTo(BuildContext cxt, File tmp) {
		File under = tmp;
		if (prepend != null)
			under = new File(under, prepend);
		return new File(under, file);
	}

}
