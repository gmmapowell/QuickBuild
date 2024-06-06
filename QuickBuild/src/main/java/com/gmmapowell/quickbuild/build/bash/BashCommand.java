package com.gmmapowell.quickbuild.build.bash;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.build.CareAboutPropagatedDirtyness;
import com.gmmapowell.quickbuild.build.ExecutesInDirCommand;
import com.gmmapowell.quickbuild.build.MayPropagateDirtyness;
import com.gmmapowell.quickbuild.build.java.JUnitEnvCommand;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.DoubleQuickCommand;
import com.gmmapowell.quickbuild.config.NotFatalCommand;
import com.gmmapowell.quickbuild.config.ProducesCommand;
import com.gmmapowell.quickbuild.config.ReadsFileCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;

public class BashCommand extends AbstractBuildCommand implements ExecutesInDirCommand, CanBeSkipped, CareAboutPropagatedDirtyness {
	private String scriptName;
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private File execdir;
	private final List<String> args = new ArrayList<String>();
	private File bashPath;
	private boolean doubleQuick = false;
	private BashDirectoryCommand dir;
	private final Set<BuildResource> analysis = new HashSet<BuildResource>();
	private final Set<File> readsFiles = new HashSet<File>();
	private BuildStatus errorReturn = BuildStatus.BROKEN;
	private Map<String, String> envs = new TreeMap<>();
	
	public BashCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "scriptName", "script to run"));
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
			opt.applyTo(config);
			if (opt instanceof ArgCommand)
				args.add(((ArgCommand)opt).getArg());
			else if (opt instanceof ResourceCommand)
				needs(((ResourceCommand)opt).getPendingResource());
			else if (opt instanceof ProducesCommand)
			{
				ProducesCommand bpc = (ProducesCommand)opt;
				bpc.execdir(execdir);
				bpc.applyTo(config);
				BuildResource jr = bpc.getProducedResource(this);
				builds.add(jr);
				if (bpc.doAnalysis())
				{
					jr.enableAnalysis();
					analysis.add(jr);
				}
			}
			else if (opt instanceof BashDirectoryCommand)
				dir = (BashDirectoryCommand) opt;
			else if (opt instanceof ReadsFileCommand)
				readsFiles.add(((ReadsFileCommand)opt).getPath());
			else if (opt instanceof DoubleQuickCommand)
				doubleQuick = true;
			else if (opt instanceof NotFatalCommand)
				errorReturn  = BuildStatus.TEST_FAILURES;
			else if (opt instanceof JUnitEnvCommand) {
				JUnitEnvCommand env = (JUnitEnvCommand) opt;
				envs.put(env.getVar(), env.getValue());
			} else if (!super.handleOption(config, opt))
				throw new UtilException("The option " + opt + " is not supported");
		}
		String os = config.getVar("os");
		if (os.equals("windows") || os.equals("win7"))
		{
			bashPath = config.getPath("bashexe");
		}
		builds.add(new BashResource(this, null, this.identifier()));

		return this;
	}

	@Override
	public boolean skipMe(BuildContext cxt) {
		return (doubleQuick && cxt.doubleQuick) || !isApplicable();
	}

	@Override
	public String getExecDir() {
		if (dir == null)
			return execdir.getPath();
		return dir.getDirectory();
	}

	@Override
	public String identifier() {
		// I don't think this is identification enough, but what would be?  An Id?  A hash of all the things it produces?
		return "Bash["+scriptName+"-"+args+"]";
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
	public OrderedFileList sourceFiles() {
		OrderedFileList ret = new OrderedFileList();
		ret.add(new File(scriptName));
		for (File f : readsFiles) {
			if (f.isDirectory()) {
				for (File g : FileUtils.findFilesMatching(f, "*"))
					if (g.isFile())
						ret.add(g);
			} else
				ret.add(f);
		}
		return ret;
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (!isApplicable())
			return BuildStatus.SKIPPED;
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
			exec.executeInDir(new File(dir.getDirectory()));

		for (String a : args)
			exec.arg(a);
		
		for (Entry<String, String> e : envs.entrySet())
			exec.env(e.getKey(), e.getValue());
		
		exec.executeInDir(execdir);
		
		exec.execute();
		if (exec.getExitCode() == 0)
		{
			for (BuildResource br : builds)
				cxt.builtResource(br, analysis.contains(br));
			return BuildStatus.SUCCESS;
		}
		else
		{
			System.out.println(exec.getStdout());
			System.out.println(exec.getStderr());
			return errorReturn;
		}
	}
	
	@Override
	public String toString() {
		return "Bash[" + scriptName + "-"+args+"]";
	}

	@Override
	public boolean makesDirty(MayPropagateDirtyness dependency) {
		for (PendingResource pr : this.needsResources()) {
			Tactic t = pr.getBuiltBy();
			if (t instanceof MayPropagateDirtyness && ((MayPropagateDirtyness)t).dirtynessPropagates())
				return true;
		}
		return false;
	}
}
