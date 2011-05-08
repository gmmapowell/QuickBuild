package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class DevenvCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {

	private String projectName;
	private File rootdir;
	private List<Tactic> tactics = new ArrayList<Tactic>();
	private OrderedFileList sources;
	private ResourcePacket builds = new ResourcePacket();
	private StructureHelper files;
	private MsResource resource;

	@SuppressWarnings("unchecked")
	public DevenvCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}


	@Override
	public void addChild(ConfigApplyCommand obj) {
		
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootdir, config.getOutput());
		tactics.add(this);
		sources = new OrderedFileList(rootdir, "*.cs");
		sources.add(rootdir, "*.xaml");
		sources.add(rootdir, "*.csproj");
		sources.add(rootdir, "*.sln");
		resource = new MsResource(this, rootdir, projectName);
		builds.add(resource);
		return this;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		CsNature nature = cxt.getNature(CsNature.class);
		if (nature == null || !nature.isAvailable())
		{
			System.out.println("CsNature not available ... skipping build");
			for (BuildResource br : builds)
				cxt.resourceAvailable(br);
			return BuildStatus.SKIPPED;
		}
		RunProcess proc = new RunProcess(nature.getDevenv());
		proc.debug(showDebug);
		proc.captureStderr();
		proc.captureStdout();
		proc.arg(files.getRelative(projectName+".sln").getPath());
		proc.arg("/rebuild");
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.resourceAvailable(resource);
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStdout());
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "Devenv["+projectName+"]";
	}

	@Override
	public ResourcePacket needsResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket providesResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return sources;
	}

	@Override
	public boolean onCascade() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String toString() {
		return identifier();
	}

}
