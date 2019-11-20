package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import org.zinutils.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

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
			addToFileList(f);
			return true;
		}

		return false;
	}

	private void addToFileList(File f) {
		if (mainSourceFileList == null)
			mainSourceFileList = new OrderedFileList();
		for (File g: FileUtils.findFilesMatching(f, "*"))
			if (g.isFile())
				mainSourceFileList.add(g);
	}

	@Override
	protected ArchiveCommand createAssemblyCommand(OrderedFileList ofl) {
		return new PDEAssembleCommand(this, files, targetName, pdelibs, includePackages, excludePackages, ofl);
	}
}
