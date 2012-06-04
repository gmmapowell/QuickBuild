package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import com.gmmapowell.bytecode.ByteCodeFile;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.ErrorCase;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JUnitRunCommand implements Tactic, DependencyFloat {
	private final File srcdir;
	private File bindir;
	private File errdir;
	
	// TODO: this is currently unused ... it should be, I think, for Android
	private final BuildClassPath bootclasspath = new BuildClassPath();
	private final Strategem parent;
	private final JavaBuildCommand jbc;
	private ResourcePacket<PendingResource> addlResources = new ResourcePacket<PendingResource>();
	private final StructureHelper files;
	private JUnitResource writeTo;


	public JUnitRunCommand(Strategem parent, StructureHelper files, JavaBuildCommand jbc) {
		this.parent = parent;
		this.files = files;
		this.jbc = jbc;
		this.srcdir = new File(files.getBaseDir(), "src/test/java");
		this.bindir = files.getOutput("test-classes");
		this.errdir = files.getOutput("test-results");
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (writeTo != null)
			writeTo.getFile().delete();
		RunClassPath classpath = new RunClassPath(jbc);
		if (addlResources != null)
			for (BuildResource r : addlResources)
			{
				classpath.add(((PendingResource) r).getPath());
			}
		RunProcess proc = new RunProcess("java");
		proc.executeInDir(files.getBaseDir());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();

		// TODO: use bootclasspath
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		proc.arg("org.junit.runner.JUnitCore");
		boolean any = false;
		for (File f : FileUtils.findFilesUnderMatching(srcdir, "*.java"))
		{
			String qualifiedName = FileUtils.convertToDottedNameDroppingExtension(f);
			File clsFile = new File(bindir, FileUtils.ensureExtension(f, ".class").getPath());
			ByteCodeFile bcf = new ByteCodeFile(clsFile, qualifiedName);
			if (bcf.hasMethodsWithAnnotation("org.junit.Test"))
			{
				any = true;
				proc.arg(qualifiedName);
			}
		}
		if (!any)
		{
			reportSuccess(cxt);
			return BuildStatus.SKIPPED;
		}
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			reportSuccess(cxt);
			return BuildStatus.SUCCESS;
		}
//		System.out.println(" !! JUnit Test Errors will be presented at end");
		
		return handleFailure(cxt, proc);
	}

	private void reportSuccess(BuildContext cxt) {
		try
		{
			if (writeTo != null)
			{
				writeTo.getFile().createNewFile();
				cxt.builtResource(writeTo);
			}
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	private BuildStatus handleFailure(BuildContext cxt, RunProcess proc) {
		ErrorCase failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
		FileUtils.assertDirectory(errdir);
		FileUtils.createFile(new File(errdir, "stdout"), proc.getStdout());
		FileUtils.createFile(new File(errdir, "stderr"), proc.getStderr());
		LinePatternParser lpp = new LinePatternParser();
		lpp.matchAll("([.E]*)", "summary", "details");
		lpp.matchAll("([0-9]+\\) [a-zA-Z0-9_.()]+)", "case", "name");
		
		int cnt = 0;
		for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStdout())))
		{
			String s;
			if (lpm.is("summary"))
			{
				s = lpm.get("details");
			}
			else if (lpm.is("case"))
			{
				s = lpm.get("name");
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
			if (s != null && s.trim().length() > 0)
			{
				System.out.println("    " + s);
				failure.addMessage(s);
			}
		}
		if (cnt > 0)
			return BuildStatus.RETRY;
		return BuildStatus.TEST_FAILURES;
	}

	@Override
	public String toString() {
		return "JUnit Runner: " + srcdir;
	}
	
	public void addToBootClasspath(File resource) {
		bootclasspath.add(resource);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	public void addLibs(List<PendingResource> junitLibs) {
		if (junitLibs.isEmpty())
			return;
		
		for (PendingResource r : junitLibs)
			addlResources.add(r);
	}

	@Override
	public ResourcePacket<PendingResource> needsAdditionalBuiltResources() {
		return addlResources;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "junit");
	}

	public void writeTo(JUnitResource jur) {
		this.writeTo = jur;
	}

}
