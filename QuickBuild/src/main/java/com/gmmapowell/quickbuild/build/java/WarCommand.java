package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;

public class WarCommand extends JarCommand {
	private List<PendingResource> warlibs = new ArrayList<PendingResource>();
	private List<Pattern> warexcl = new ArrayList<Pattern>();
	private List<WarRandomFileCommand> warfiles = new ArrayList<WarRandomFileCommand>();
	private WarResource warResource;

	public WarCommand(TokenizedLine toks) {
		super(toks);
	}

	@Override
	public boolean processOption(ConfigApplyCommand cmd) {
		if (cmd instanceof WarLibCommand)
		{
			warlibs.add(((WarLibCommand)cmd).getPendingResource());
			return true;
		}
		else if (cmd instanceof ExcludeCommand)
		{
			warexcl.add(((ExcludeCommand)cmd).getPattern());
			return true;
		}
		else if (cmd instanceof WarRandomFileCommand)
		{
			warfiles.add((WarRandomFileCommand)cmd);
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
		warResource = new WarResource(this, files.getOutput(targetName));
		WarBuildCommand cmd = new WarBuildCommand(this, files, warResource, targetName, warlibs, warfiles, warexcl);
		cmd.add(new File(files.getOutputDir(), "classes"));
		cmd.add(files.getRelative("src/main/resources"));
		tactics.add(cmd);
		jarResource = null;
		willProvide.add(warResource);
		
		for (WarRandomFileCommand wrf : warfiles)
			needsResources.add(wrf.getPendingResource());
	}

	@Override
	public String identifier() {
		return "War[" + targetName + "]";
	}

}
