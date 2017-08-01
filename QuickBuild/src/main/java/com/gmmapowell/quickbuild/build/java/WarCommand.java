package com.gmmapowell.quickbuild.build.java;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.zinutils.parser.TokenizedLine;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.DirectoryResourceCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.PendingResource;

import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public class WarCommand extends JarCommand {
	private List<PendingResource> warlibs = new ArrayList<PendingResource>();
	private List<DirectoryResourceCommand> wardirs = new ArrayList<DirectoryResourceCommand>();
	private List<Pattern> warexcl = new ArrayList<Pattern>();
	private boolean alsoJar = false;
	private ArchiveCommand jarCommand;
	
	public WarCommand(TokenizedLine toks) {
		super(toks);
	}

	@Override
	public boolean processOption(ConfigApplyCommand cmd) {
		if (cmd instanceof ExcludeCommand)
		{
			warexcl.add(((ExcludeCommand)cmd).getPattern());
			return true;
		}
		else if (cmd instanceof ResourceCommand)
		{
			PendingResource pr = ((ResourceCommand)cmd).getPendingResource();
			warlibs.add(pr);
			needsResources.add(pr);
			addJUnitLib(pr);
			return true;
		}
		else if (cmd instanceof DirectoryResourceCommand)
		{
			DirectoryResourceCommand opt = (DirectoryResourceCommand) cmd;
			wardirs.add(opt);
			addJUnitLib(opt.getResource());
			return true;
		} else if (cmd instanceof AlsoJarCommand) {
			alsoJar = true;
			return true;
		} else
			return false;
	}
	
	
	@Override
	protected ArchiveCommand createAssemblyCommand(OrderedFileList ofl) {
		if (alsoJar) {
			jarCommand = super.createAssemblyCommand(ofl);
			return jarCommand;
		} else
			return createWarCommand(ofl);
	}
	
	@Override
	protected void additionalCommands(Config config, OrderedFileList ofl) {
		if (alsoJar) {
			ArchiveCommand war = createWarCommand(ofl);
			war.addProcessDependency(jarCommand);
			tactics.add(war);
		}
	}

	protected ArchiveCommand createWarCommand(OrderedFileList ofl) {
		targetName = FileUtils.ensureExtension(targetName, ".war");
		// Remove the jar command
//		for (Tactic t : tactics)
//			if (t instanceof JarBuildCommand)
//			{
//				tactics.remove(t);
//				break;
//			}
		WarBuildCommand cmd = new WarBuildCommand(this, files, targetName, warlibs, wardirs, warexcl, ofl, gitIdCommand);
		cmd.builds(cmd.getResource());
		return cmd;
	}

	@Override
	public String identifier() {
		return "War[" + targetName + "]";
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("War " + targetName);
		return sb.toString();
	}
}
