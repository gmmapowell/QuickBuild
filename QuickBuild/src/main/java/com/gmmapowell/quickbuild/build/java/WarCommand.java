package com.gmmapowell.quickbuild.build.java;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;

public class WarCommand extends JarCommand {
	private List<PendingResource> warlibs = new ArrayList<PendingResource>();
	private List<Pattern> warexcl = new ArrayList<Pattern>();

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
			return true;
		}

		return false;
	}
	
	@Override
	protected void additionalCommands(Config config) {
		super.additionalCommands(config);
		targetName = FileUtils.ensureExtension(targetName, ".war");
		// Remove the jar command
		for (Tactic t : tactics)
			if (t instanceof JarBuildCommand)
			{
				tactics.remove(t);
				break;
			}
		for (BuildResource br : willProvide) {
			if (br instanceof JarResource)
			{
				willProvide.remove(br);
				break;
			}
		}
		WarBuildCommand cmd = new WarBuildCommand(this, files, targetName, warlibs, warexcl, gitIdCommand);
		tactics.add(cmd);
		willProvide.add(cmd.getResource());
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
