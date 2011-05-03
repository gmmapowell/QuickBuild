package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class JavaDocCommand extends NoChildCommand implements ConfigBuildCommand, Strategem, Tactic {

	private final File rootdir;
	private File outputdir;

	public JavaDocCommand(TokenizedLine toks) {
		toks.process(this);
		this.rootdir = FileUtils.getCurrentDir();
	}


	@Override
	public Strategem applyConfig(Config config) {
		outputdir = new File(rootdir, "javadoc");
		return this;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public String toString() {
		return "Javadoc";
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JavaNature nature = cxt.getNature(JavaNature.class);
		if (nature == null)
			throw new UtilException("There is no JavaNature installed (huh?)");
		
		BuildClassPath classpath = new BuildClassPath();
		for (BuildResource br : cxt.getResources(JarResource.class))
		{
			File path = ((JarResource)br).getPath();
			classpath.add(path);
		}
		
		BuildClassPath sourcepath = new BuildClassPath();
		Set<String> packages = new TreeSet<String>();
		for (BuildResource br : cxt.getResources(JavaSourceDirResource.class))
		{
			File path = ((JavaSourceDirResource)br).getPath();
			sourcepath.add(path);
			for (File f : FileUtils.findFilesUnderMatching(path, "*.java"))
				packages.add(FileUtils.convertToDottedName(f.getParentFile()));
		}
		if (sourcepath.empty())
			return BuildStatus.SKIPPED;

		RunProcess proc = new RunProcess("javadoc");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
	
		proc.arg("-d");
		proc.arg(outputdir.getPath());
		proc.arg("-sourcepath");
		proc.arg(sourcepath.toString());
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		boolean any = false;
		for (String f : packages)
		{
			proc.arg(f);
			any = true;
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "JavaDoc[]";
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
		// maybe javadoc?
		return new ResourcePacket();
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		return CollectionUtils.listOf(this);
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList();
	}

	@Override
	public boolean onCascade()
	{
		return true;
	}
}
