package com.gmmapowell.quickbuild.build.bash;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class BashCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {
	private String scriptName;
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final List<Tactic> tactics = new ArrayList<Tactic>();
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private File execdir;
	private final List<String> args = new ArrayList<String>();
	private File bashPath;
	private BashDirectoryCommand dir;
	
	@SuppressWarnings("unchecked")
	public BashCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "scriptName", "script to run"));
		tactics.add(this);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public Strategem applyConfig(Config config) {
		execdir = FileUtils.getCurrentDir();
		for (ConfigApplyCommand opt : options)
		{
			if (opt instanceof ArgCommand)
				args.add(((ArgCommand)opt).getArg());
			else if (opt instanceof BashResourceCommand)
				needs.add(((BashResourceCommand)opt).getPendingResource());
			else if (opt instanceof BashProducesCommand)
			{
				BashProducesCommand bpc = (BashProducesCommand)opt;
				bpc.applyTo(config);
				builds.add(bpc.getProducedResource(this));
			}
			else if (opt instanceof BashDirectoryCommand)
				dir = (BashDirectoryCommand) opt;
			else
				throw new UtilException("The option " + opt + " is not supported");
		}
		String os = config.getVar("os");
		if (os.equals("windows") || os.equals("win7"))
		{
			bashPath = config.getPath("bashexe");
		}

		return this;
	}

	@Override
	public String identifier() {
		// I don't think this is identification enough, but what would be?  An Id?  A hash of all the things it produces?
		return "Bash["+scriptName+"-"+args+"]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return provides;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return execdir;
	}

	@Override
	public List<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public OrderedFileList sourceFiles() {
		OrderedFileList ret = new OrderedFileList();
		ret.add(new File(scriptName));
		return ret;
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunProcess exec = null;
		if (bashPath != null)
		{
			exec = new RunProcess(bashPath.getPath());
			exec.arg(scriptName);
		}
		else
			exec = new RunProcess(scriptName);
		new RunProcess(scriptName);
		exec.debug(showDebug);
		exec.showArgs(showArgs);
		exec.captureStdout();
		exec.captureStderr();
		if (dir != null)
			exec.executeInDir(new File(dir.dir));

		for (String a : args)
			exec.arg(a);
		exec.executeInDir(execdir);
		
		exec.execute();
		if (exec.getExitCode() == 0)
		{
			for (BuildResource br : builds)
				cxt.builtResource(br, false); // todo: should be an option on BashProducesCommand
			return BuildStatus.SUCCESS;
		}
		else
		{
			System.out.println(exec.getStdout());
			System.out.println(exec.getStderr());
			return BuildStatus.BROKEN;
		}
	}
	
	@Override
	public String toString() {
		return "Bash[" + scriptName + "-"+args+"]";
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

}
