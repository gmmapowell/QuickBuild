package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.zinutils.bytecode.ByteCodeFile;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import org.zinutils.parser.LinePatternParser.MatchIterator;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.build.ErrorCase;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

import org.zinutils.sync.SyncUtils;
import org.zinutils.system.RunProcess;
import org.zinutils.system.ThreadedStreamReader;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public class JUnitRunCommand extends AbstractTactic implements CanBeSkipped {
//	private final File srcdir;
//	private final File golddir;
//	private File bindir;
//	private File goldbin;
	private File errdir;
	
	private final BuildClassPath bootclasspath = new BuildClassPath();
	private final BuildClassPath classpath = new BuildClassPath();
	private final List<JavaBuildCommand> testBuilds;
	private final StructureHelper files;
	private final List<String> defines = new ArrayList<String>();
	private final List<Pattern> onlyMatchingPattern = new ArrayList<Pattern>();
	private String memory;
	private final String idAs;
	private final OrderedFileList testResourceFiles;
	private File dumpClasspath;

	public JUnitRunCommand(Strategem parent, StructureHelper files, List<JavaBuildCommand> testBuilds, OrderedFileList testResourceFiles) {
		super(parent);
		this.files = files;
		this.testBuilds = testBuilds;
		this.testResourceFiles = testResourceFiles;
		this.errdir = files.getOutput("test-results");
		this.idAs = parent.rootDirectory().getName();
	}

	public void setJUnitMemory(String junitMemory) {
		this.memory = junitMemory;
	}

	public void define(String d) {
		this.defines.add(d);
	}

	public void pattern(String p) {
		this.onlyMatchingPattern.add(Pattern.compile(p));
	}

	@Override
	public OrderedFileList sourceFiles() {
		return testResourceFiles;
	}

	public void dumpClasspathTo(File output) {
		dumpClasspath = output;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (cxt.doubleQuick)
			return BuildStatus.SKIPPED;
		RunClassPath classpath = new RunClassPath(cxt, testBuilds);
		for (File f : this.classpath)
			classpath.add(f);
		for (BuildResource r : needsResources())
			classpath.add(r.getPath());
		for (BuildResource f : cxt.getTransitiveDependencies(this))
			if (f != null && !(f instanceof ProcessResource))
				classpath.add(f.getPath());

		if (dumpClasspath != null) {
			try {
				PrintWriter pw = new PrintWriter(dumpClasspath);
				pw.println(classpath.toString());
				pw.close();
			} catch (IOException ex) {
				System.out.println("Could not write classpath to " + dumpClasspath);
			}
		}
		
		// Collect list of tests to run ...
		List<String> testsToRun = new ArrayList<String>();
		for (JavaBuildCommand jbc : testBuilds)
			addTestsFrom(testsToRun, jbc.getSourceDir(), jbc.getOutputDir());

		if (testsToRun.isEmpty())
		{
			return BuildStatus.SKIPPED;
		}
		Collections.sort(testsToRun);

		new File(errdir, "stdout").delete();
		new File(errdir, "stderr").delete();
		BuildStatus ret = BuildStatus.SUCCESS;
		
		for (String t : testsToRun) {
			List<String> oneTest = CollectionUtils.listOf(t);
			RunProcess proc = runTestBatch(cxt, showArgs, showDebug, classpath, oneTest);
			if (proc.getExitCode() != 0)
			{
				handleFailure(cxt, proc);
				ret = BuildStatus.TEST_FAILURES;
			}
			proc.destroy();
		}
		
		return ret;
	}

	protected void addTestsFrom(List<String> testsToRun, File dir, File bin) {
		if (dir.isDirectory())
			for (File f : FileUtils.findFilesUnderMatching(dir, "*.java"))
				addTests(testsToRun, f, bin);
	}

	protected void addTests(List<String> testsToRun, File clz, File bin) {
		String qualifiedName = FileUtils.convertToDottedNameDroppingExtension(clz);
		File clsFile = new File(bin, FileUtils.ensureExtension(clz, ".class").getPath());
		ByteCodeFile bcf = checkIfContainsTests(qualifiedName, clsFile, bin);
		if (bcf != null && bcf.isConcrete())
			testsToRun.add(qualifiedName);
	}

	protected ByteCodeFile checkIfContainsTests(String qualifiedName, File clsFile, File bin) {
		ByteCodeFile bcf = new ByteCodeFile(clsFile, qualifiedName);
		if (bcf.hasClassAnnotation("org.junit.runner.RunWith") || bcf.hasMethodsWithAnnotation("org.junit.Test"))
		{
			if (!onlyMatchingPattern.isEmpty()) {// then it must match at least one of the patterns
				boolean found = false;
				for (Pattern s : onlyMatchingPattern)
					found |= s.matcher(qualifiedName).find();
				if (!found)
					return null;
			}
			return bcf;
		}
		
		String sc = bcf.getSuperClass();
		if (sc != null && !sc.equals("java/lang/Object")) {
			File f = new File(sc);
			File f2 = new File(bin, FileUtils.ensureExtension(f, ".class").getPath());
			if (f2.exists() && checkIfContainsTests(FileUtils.convertToDottedName(f), f2, bin) != null)
				return bcf;
		}
		return null;
	}

	public RunProcess runTestBatch(BuildContext cxt, boolean showArgs, boolean showDebug, RunClassPath classpath, List<String> testsToRun) {
		RunProcess proc = new RunProcess("java");
		proc.executeInDir(files.getBaseDir());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		ThreadedStreamReader stdout = proc.captureStdout();
		ThreadedStreamReader stderr = proc.captureStderr();
		FileUtils.assertDirectory(errdir);
		stdout.appendTo(new File(errdir, "stdout"));
		stderr.appendTo(new File(errdir, "stderr"));
		HandleError handleError = new HandleError(stderr);
		stdout.parseLines(stdoutParser(), new HandleOutput(cxt.output, handleError));

		if (!bootclasspath.empty())
		{
			proc.arg("-Xbootclasspath:" + bootclasspath.toString());
		}
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		for (String s : defines)
			proc.arg(s);
		if (memory != null)
			proc.arg("-Xmx" + memory);
		proc.arg("org.zinutils.test.QBJUnitRunner");
		if (!cxt.allTests) // should be a flag
			proc.arg("--quick");
		for (String s : testsToRun)
			proc.arg(s);
		proc.execute();
		return proc;
	}

	private BuildStatus handleFailure(BuildContext cxt, RunProcess proc) {
		ErrorCase failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
		LinePatternParser lpp = new LinePatternParser();
		lpp.matchAll("(Failure: .*)", "case", "data");
		for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStdout())))
		{
			if (lpm.is("case"))
			{
				String s = lpm.get("data");
				if (s != null && s.trim().length() > 0)
					failure.addMessage(s);
			}
		}
		return BuildStatus.TEST_FAILURES;
	}
	
	public void addToBootClasspath(File resource) {
		bootclasspath.add(resource);
	}

	public void addToClasspath(File resource) {
		classpath.add(resource);
	}

	public void addLibs(List<BuildResource> junitLibs) {
		if (junitLibs.isEmpty())
			return;
		
		for (BuildResource r : junitLibs)
			if (r instanceof PendingResource)
				needs((PendingResource) r);
			else
				addToClasspath(r.getPath());
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "junit");
	}

	@Override
	public boolean skipMe(BuildContext cxt) {
		return cxt.doubleQuick;
	}

	@Override
	public String toString() {
		return "Testing " + idAs;
	}
	
	private LinePatternParser stdoutParser() {
		LinePatternParser lpp = new LinePatternParser();
		lpp.matchAll("Running batch (.*)", "startBatch", "details");
		lpp.matchAll("Ran batch (.*)", "endBatch", "details");
		lpp.matchAll("Starting test (.*)", "startTest", "name");
		lpp.matchAll("Ignoring test (.*)", "ignoreTest", "name");
		lpp.matchAll("Failure: (.*)", "failure", "name");
		lpp.matchAll("Duration: ([0-9]*)", "duration", "ms");
		lpp.matchAll("(Summary of .*)", "summary", "info");
		return lpp;
	}
	
	private class HandleOutput implements MatchIterator {
		String currentTest = null;
		int failed = 0;
		private final BuildOutput output;
		private HandleError handleError = null;
		
		private HandleOutput(BuildOutput output, HandleError handleError) {
			this.output = output;
			if (output.forTeamCity())
				this.handleError = handleError;
		}

		@Override
		public void handleMatch(LinePatternMatch lpm) {
			if (lpm.is("startBatch"))
			{
				output.startTestBatch(lpm.get("details"));
			}
			else if (lpm.is("endBatch"))
			{
				output.endTestBatch(lpm.get("details"));
			}
			else if (lpm.is("startTest"))
			{
				currentTest = lpm.get("name");
				output.startTest(currentTest);
			}
			else if (lpm.is("ignoreTest"))
			{
				currentTest = null;
				output.ignoreTest(lpm.get("name"));
			}
			else if (lpm.is("failure"))
			{
				String messageAndStackTrace = null;
				if (handleError != null)
					messageAndStackTrace = handleError.tellMeAbout(output, lpm.get("name"));
				output.failTest(lpm.get("name"), messageAndStackTrace);
				failed++;
			}
			else if (lpm.is("duration"))
			{
				output.finishTest(currentTest, Integer.parseInt(lpm.get("ms")));
				if (handleError != null)
					handleError.fineWith(currentTest);
				currentTest = null;
			}
			else if (lpm.is("summary"))
			{
				if (failed > 0)
					output.testSummary(lpm.get("info"));
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}
	}

	public class HandleError implements MatchIterator {
		private LinePatternParser lpp;
		private StringBuilder copyStderr;
		private StringBuilder messageAndStack;
		private String currentCase;
		private boolean collectTrace = false;
		private Set<String> decline = new HashSet<String>();
		private Map<String, StderrCopy> cases = new HashMap<String, StderrCopy>();

		public HandleError(ThreadedStreamReader stderr) {
			lpp = new LinePatternParser();
			lpp.match("Starting test (.*)", "start", "case");
			lpp.match("Finished test", "finished");
			lpp.match("(.*)", "always", "line");
			lpp.match("Failed test", "traceMarker");
			stderr.parseLines(lpp, this);
		}

		public void fineWith(String test) {
			decline.add(test);
			if (cases.containsKey(test))
				cases.remove(test);
		}

		public String tellMeAbout(BuildOutput output, String test) {
			if (decline.contains(test))
				throw new UtilException("You declined " + test + " already");
			synchronized (this) {
				while (test.equals(currentCase) || !cases.containsKey(test))
					SyncUtils.waitFor(this, 0);
				StderrCopy thisCase = cases.get(test);
				if (thisCase.copyStderr.length() > 0)
					output.testStderr(test, thisCase.copyStderr.toString());
				return thisCase.messageAndStack.toString();
			}
		}

		@Override
		public void handleMatch(LinePatternMatch lpm) {
			if (lpm.is("start")) {
				synchronized (this) {
					currentCase = lpm.get("case");
					copyStderr = new StringBuilder();
					messageAndStack = new StringBuilder();
					if (!decline.contains(currentCase)) {
						cases.put(currentCase, new StderrCopy(copyStderr, messageAndStack));
					}
				}
			} else if (currentCase == null)
				return;
			
			if (lpm.is("finished")) {
				synchronized (this) {
					currentCase = null;
					this.notify();
				}
			}
			else if (lpm.is("always")) {
				String msg = lpm.get("line") + "\n";
				copyStderr.append(msg);
				if (collectTrace)
					messageAndStack.append(msg);
			} else if (lpm.is("traceMarker"))
				collectTrace = true;
		}
	}

	public class StderrCopy {
		private final StringBuilder copyStderr;
		private final StringBuilder messageAndStack;

		public StderrCopy(StringBuilder copyStderr,	StringBuilder messageAndStack) {
			this.copyStderr = copyStderr;
			this.messageAndStack = messageAndStack;
		}

	}
}
