package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

public class JackBuildCommand extends AbstractTactic {
	private final AndroidContext acxt;
	private final File bindir;
	private final File dexDir;
	private final List<File> jars = new ArrayList<File>();
	private final File libdir;
	private final Set<Pattern> exclusions;
	private ResourcePacket<PendingResource> uselibs;
	private final File jillDir;

	public JackBuildCommand(AndroidContext acxt, Strategem parent, StructureHelper files, File bindir, File libdir, File dexDir, File jillDir, Set<Pattern> exclusions, ResourcePacket<PendingResource> uselibs) {
		super(parent);
		this.acxt = acxt;
		this.bindir = bindir;
		this.libdir = libdir;
		this.dexDir = dexDir;
		this.jillDir = jillDir;
		this.exclusions = exclusions;
		this.uselibs = uselibs;
	}

	public void addJar(File file) {
		jars.add(file);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (uselibs != null)
		{
			for (PendingResource pr : uselibs)
			{
				File path = pr.physicalResource().getPath();
				addJar(path);
			}
			uselibs = null;
		}
		
		LinkedHashSet<String> paths = new LinkedHashSet<String>();
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource)
				considerAdding(paths, br.getPath().getPath());
		}
		
		for (File f : FileUtils.findFilesMatching(libdir, "*.jar"))
			considerAdding(paths, f.getPath());
		for (File f : jars)
			considerAdding(paths, f.getPath());

		List<String> jills = new ArrayList<String>();
		for (String s : paths) {
			File jp = new File(jillDir, FileUtils.ensureExtension(new File(s).getName(), ".jack"));
			jills.add(jp.getPath());
			if (!jp.exists() || jp.lastModified() < new File(s).lastModified()) {
				RunProcess proc = new RunProcess("java");
				proc.captureStdout();
				proc.captureStderr();
//				proc.debug(true);
	//			proc.arg("-Djack.dex.output.container=file");
				proc.arg("-jar");
				proc.arg(acxt.getJill().getPath());
				proc.arg(s);
				proc.arg("--output");
				proc.arg(jp.getPath());
				proc.execute();
			}
		}
		
		RunProcess proc = new RunProcess("java");
//		proc.arg("-Djack.dex.output.container=file");
		proc.arg("-jar");
		proc.arg(acxt.getJack().getPath());
		proc.captureStdout();
		proc.captureStderr();
//		proc.showArgs(showArgs);
		proc.debug(showDebug);
		
		proc.arg("--verbose"); proc.arg("debug");
		proc.arg("--classpath"); proc.arg(acxt.getPlatformJar().getPath() + ":" + acxt.getPlatformJar().getPath());
		proc.arg("-D"); proc.arg("jack.import.resource.policy=keep-first");
		proc.arg("--output-dex");
		proc.arg(dexDir.getPath());
//		proc.arg("--import");
//		proc.arg(bindir.getPath());
//		
//		for (String s : paths) {
//			proc.arg("--import");
//			proc.arg(s);
//		}
		
		proc.arg("/tmp/chaddyAndroid/src/main/java");
		proc.arg("/tmp/chaddyAndroid/src/android/gen");
		proc.execute();
		if (proc.getStderr().length() > 0 || proc.getStdout().length() > 0)
		{
			if (!showDebug) {
				System.out.println(proc.getStdout());
				System.out.println(proc.getStderr());
			}
			return BuildStatus.BROKEN;
		}
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		if (!showDebug)
			System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}
	
	private void considerAdding(LinkedHashSet<String> paths, String path) {
		for (Pattern patt : exclusions)
			if (patt.matcher(path.toLowerCase().replaceAll("\\\\", "/")).matches())
				return;
		paths.add(path);
	}

	@Override
	public String toString() {
		return "Jack: " + dexDir;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "jack");
	}
}
