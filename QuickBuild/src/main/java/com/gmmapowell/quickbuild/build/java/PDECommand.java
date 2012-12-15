package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;

public class PDECommand extends JarCommand {
	private List<File> pdelibs = new ArrayList<File>();

	public PDECommand(TokenizedLine toks) {
		super(toks);
	}

	@Override
	public boolean processOption(ConfigApplyCommand cmd) {
		if (cmd instanceof ResourceCommand)
		{
			String name = ((ResourceCommand)cmd).getPendingResource().getPending();
			File f = files.getRelative(name);
			pdelibs.add(f);
			return true;
		}

		return false;
	}

	@Override
	protected ArchiveCommand createAssemblyCommand() {
		return new PDEAssembleCommand(this, files, targetName, pdelibs, includePackages, excludePackages);
	}
}
