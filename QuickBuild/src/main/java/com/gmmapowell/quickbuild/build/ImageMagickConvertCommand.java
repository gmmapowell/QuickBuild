package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
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

public class ImageMagickConvertCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {
	private String ruleset;
	private String project;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final OrderedFileList sources = new OrderedFileList();
	private final List<Tactic> tactics = new ArrayList<Tactic>();
	private boolean canWork;
	private File imagick;
	private File projectDir;

	@SuppressWarnings("unchecked")
	public ImageMagickConvertCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "ruleset", "set of rules to use"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "project", "project root dir")
		);
		projectDir = new File(project);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public Strategem applyConfig(Config config) {
		if (config.hasPath("imagick"))
		{
			canWork = true;
			imagick = config.getPath("imagick");
			
			for (ConfigApplyCommand opt : options)
			{
				if (opt instanceof ImageMagickLauncherIcon)
				{
					sources.add(((ImageMagickLauncherIcon)opt).getSource());
				}
				else if (opt instanceof ImageMagickNotificationIcon)
				{
					sources.add(((ImageMagickNotificationIcon)opt).getSource());
				}
				else
					throw new UtilException("ImageMagick cannot handle " + opt);
			}
		}
		tactics.add(this);
		return this;
	}
	
	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs,
			boolean showDebug) {
		if (!canWork)
			return BuildStatus.NOTAPPLICABLE;
		
		BuildStatus ret = BuildStatus.SUCCESS;
		for (ConfigApplyCommand opt : options)
		{
			if (opt instanceof ImageMagickLauncherIcon)
			{
				ImageMagickLauncherIcon launcher = (ImageMagickLauncherIcon) opt;
				File source = launcher.getSource();
				String name = launcher.getCalled();
				ret = resizeImage(showArgs, showDebug, ret, source, 96, 96, "xhdpi-v9", name);
				ret = resizeImage(showArgs, showDebug, ret, source, 72, 72, "hdpi-v9", name);
				ret = resizeImage(showArgs, showDebug, ret, source, 48, 48, "mdpi-v9", name);
				ret = resizeImage(showArgs, showDebug, ret, source, 32, 32, "ldpi-v9", name);
				ret = resizeImage(showArgs, showDebug, ret, source, 32, 32, null, name);
			}
			else if (opt instanceof ImageMagickNotificationIcon)
			{
				ImageMagickNotificationIcon launcher = (ImageMagickNotificationIcon) opt;
				File source = launcher.getSource();
				String name = launcher.getCalled();
				ret = resizeImage(showArgs, showDebug, ret, source, 25, 25, null, name);
			}
		}
		
		return ret;
	}

	protected BuildStatus resizeImage(boolean showArgs, boolean showDebug, BuildStatus ret, File source, int i, int j, String type, String name) {
		if (type == null)
			type = "";
		else
			type = "-" + type;
		File dir = FileUtils.combine(projectDir, "src/android/res/drawable"+type);
		FileUtils.assertDirectory(dir);
		File toFile = FileUtils.combine(dir, name);
		RunProcess proc = new RunProcess(new File(imagick, "convert").getPath());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
		proc.arg("-resize");
		proc.arg(i+"x"+j);
		proc.arg(source.getAbsolutePath());
		proc.arg(toFile.getAbsolutePath());
		proc.execute();
		if (proc.getExitCode() != 0)
		{
			System.out.println(proc.getStderr());
			ret = BuildStatus.BROKEN;
		}
		return ret;
	}

	@Override
	public String identifier() {
		return "ImageMagick["+ruleset + ":" + project +"]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return sources;
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

	@Override
	public String toString() {
		return identifier();
	}

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
