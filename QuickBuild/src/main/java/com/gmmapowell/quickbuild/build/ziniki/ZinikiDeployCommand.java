package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.BuildClassPath;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

public class ZinikiDeployCommand extends AbstractTactic {
	private final File pmzPath;
	private final String name;
	private final BuildResource jarResource;
	private String mode = null;
	private boolean bootZiniki;
	private List<ZinikiReferenceCommand> refs;
	private List<ZinikiUseModuleCommand> modules;

	public ZinikiDeployCommand(Strategem parent, File pmzPath, String name) {
		super(parent);
		this.pmzPath = pmzPath;
		this.name = name;
		this.jarResource = new JarResource(this, new File(parent.rootDirectory(), "deploy/"+name+".jar"));
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void bootZiniki() {
		this.bootZiniki = true;
	}

	public void refersTo(List<ZinikiReferenceCommand> refs) {
		this.refs = refs;
		for (ZinikiReferenceCommand r : refs)
			needs(r.getResource());
	}

	public void usesModules(List<ZinikiUseModuleCommand> modules) {
		this.modules = modules;
		for (ZinikiUseModuleCommand r : modules)
			needs(r.getResource());
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		BuildClassPath classpath = new BuildClassPath();
		if (pmzPath != null) {
			for (File f : FileUtils.findFilesMatching(new File(pmzPath, "root"), "*.jar"))
				classpath.add(f);
			for (File f : FileUtils.findFilesMatching(new File(pmzPath, "libs"), "*.jar"))
				classpath.add(f);
		} else {
			for (BuildResource f : cxt.getTransitiveDependencies(this))
				if (f != null && !(f instanceof ProcessResource))
					classpath.add(f.getPath());
		}
		RunProcess proc = new RunProcess("java");
		proc.executeInDir(parent.rootDirectory());
		proc.captureStdout();
		proc.captureStderr();
		proc.showArgs(showArgs);
//		proc.showArgs(true);
		proc.debug(showDebug);
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		proc.arg("org.ziniki.tools.deploy.ZinikiDeploy");
		if (mode != null)
			proc.arg(mode);
		proc.arg("--bindir");
		proc.arg("qbout/classes");
		proc.arg("-o");
		proc.arg(name+".jar");
		proc.arg(".");
		if (!bootZiniki) {
			proc.arg("--reference");
			proc.arg(new File(pmzPath, "builtins/builtin.jar").getPath());
			proc.arg("--reference");
			proc.arg(new File(pmzPath, "privileged/datamodel.jar").getPath());
		}
		if (refs != null) {
			for (ZinikiReferenceCommand r : refs) {
				proc.arg("--reference");
				proc.arg(r.getResource().getPath().getPath());
			}
		}
		if (modules != null) {
			for (ZinikiUseModuleCommand r : modules) {
				proc.arg("--module");
				proc.arg(r.getResource().getPath().getPath());
			}
		}
		proc.execute();
		if (proc.getExitCode() == 0) {
			cxt.builtResource(jarResource, false);
			return BuildStatus.SUCCESS;
		}
		LinePatternParser lpp = new LinePatternParser();
		lpp.match("no valid definition of (.*) during whitelist", "whitelist", "class");
		lpp.match("ERROR: (.*)", "error", "msg");
		int cnt = 0;
		for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStderr())))
		{
			if (lpm.is("whitelist"))
			{
				String pkg = lpm.get("class");
				System.out.println("  Can't whitelist: " + pkg);
				cnt++;
			}
			else if (lpm.is("error"))
			{
				String msg = lpm.get("msg");
				System.out.println("  " + msg);
				cnt++;
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}
		if (cnt == 0)
			cxt.output.buildErrors(proc.getStderr());

		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "ZinikiDeploy["+parent.rootDirectory()+"]";
	}

	@Override
	public String toString() {
		return "Deploy Ziniki: " + parent.rootDirectory();
	}

	public BuildResource getResource() {
		return jarResource;
	}
}
