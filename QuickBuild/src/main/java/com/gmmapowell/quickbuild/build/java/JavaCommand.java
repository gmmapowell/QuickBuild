package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.zinutils.bytecode.ByteCodeFile;
import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.system.ThreadedStreamReader;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.build.ExecutesInDirCommand;
import com.gmmapowell.quickbuild.build.bash.BashDirectoryCommand;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.NotFatalCommand;
import com.gmmapowell.quickbuild.config.ProducesCommand;
import com.gmmapowell.quickbuild.config.ReadsFileCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;

public class JavaCommand extends AbstractBuildCommand implements ExecutesInDirCommand, CanBeSkipped {
	private String projectName;
	private String mainClass;
	private String label;
	private final File rootdir;
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private File bindir;
	private File errdir;
	
	private final BuildClassPath classpath = new BuildClassPath();
	private final List<String> defines = new ArrayList<String>();
	private final List<Pattern> onlyMatchingPattern = new ArrayList<Pattern>();
	private List<String> args = new ArrayList<String>();
	private StructureHelper files;
	private String reldir;
	private final Set<File> readsFiles = new HashSet<File>();
	private List<BuildResource> produces = new ArrayList<BuildResource>();
	private BuildStatus errorReturn = BuildStatus.BROKEN;

	public JavaCommand(TokenizedLine toks) {
		super(toks, 
			new ArgumentDefinition("--label", Cardinality.OPTION, "label", "project label"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "mainClass", "main class"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
		PendingResource needsProject = new PendingResource(projectName+"/qbout/");
		needs(needsProject);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public Strategem applyConfig(Config config) {
		config.getNature(JavaNature.class);
		files = new StructureHelper(rootdir, config.getOutput());
		this.errdir = files.getOutput("java-output" + (label != null ? "/" + label : ""));
		processOptions(config);
		return this;
	}

	private void processOptions(Config config) {
		for (ConfigApplyCommand opt : options)
		{
			opt.applyTo(config);
			if (opt instanceof ArgsCommand)
				args.addAll(((ArgsCommand)opt).args());
			else if (opt instanceof BashDirectoryCommand) {
				reldir = ((BashDirectoryCommand)opt).getDirectory();
			}
			else if (opt instanceof ReadsFileCommand)
				readsFiles.add(((ReadsFileCommand)opt).getPath());
			else if (opt instanceof ProducesCommand) {
				ProducesCommand jpc = (ProducesCommand)opt;
				if (reldir != null)
					jpc.execdir(new File(rootdir, reldir));
				else
					jpc.execdir(rootdir);
				jpc.applyTo(config);
				BuildResource resource = jpc.getProducedResource(this);
				produces.add(resource);
				this.buildsResources().add(resource);
				if (jpc.doAnalysis()) {
					resource.enableAnalysis();
				}
			}
			else if (opt instanceof JUnitLibCommand)
			{
				addJUnitLib((JUnitLibCommand)opt);
			}
			else if (opt instanceof UseComboCommand)
			{
				((UseComboCommand)opt).needComboLib(this);
			}
			else if (opt instanceof NotFatalCommand)
			{
				errorReturn  = BuildStatus.TEST_FAILURES;
			}
			else if (opt instanceof ResourceCommand)
			{
				needs(((ResourceCommand)opt).getPendingResource());
			}
			else if (opt instanceof JUnitDefineCommand)
			{
				define(((JUnitDefineCommand)opt).getDefine());
			}
			else if (!super.handleOption(config, opt))
				throw new UtilException("The option " + opt + " is not valid for JavaCommand");
		}
	}

	@Override
	public String getExecDir() {
		return reldir;
	}

	public void define(String d) {
		this.defines.add(d);
	}

	protected void addJUnitLib(JUnitLibCommand opt) {
		needs(opt.getResource());
	}

	public void addJUnitLib(BuildResource br) {
		needs(br instanceof PendingResource?(PendingResource) br:new PendingResource(br));
	}

	public void pattern(String p) {
		this.onlyMatchingPattern.add(Pattern.compile(p));
	}

	@Override
	public OrderedFileList sourceFiles() {
		OrderedFileList ret = new OrderedFileList();
		for (File useJustOnce : readsFiles) {
			File relPath = FileUtils.makeRelative(useJustOnce);
			File absPath = FileUtils.combine(rootdir, reldir, relPath);
			if (absPath.isDirectory()) {
				for (File foundFile : FileUtils.findFilesMatching(absPath, "*"))
					if (foundFile.isFile())
						ret.add(foundFile);
			} else
				ret.add(absPath);
		}
		return ret;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunClassPath classpath = new RunClassPath(cxt, null);
		for (File f : this.classpath)
			classpath.add(f);
		for (BuildResource r : needsResources()) {
			if (r instanceof JarResource)
				classpath.add(FileUtils.relativePath(r.getPath()));
			else if (r instanceof DirectoryResource)
				classpath.add(r.getPath());
			else if (r instanceof PendingResource) {
				for (File f: r.getPaths())
					classpath.add(f);
			}
		}
		for (BuildResource f : cxt.getTransitiveDependencies(this))
			if (f != null && (f instanceof JarResource))
				classpath.add(FileUtils.relativePath(f.getPath()));
		
		FileUtils.assertDirectory(errdir);
		new File(errdir, "stdout").delete();
		new File(errdir, "stderr").delete();
		BuildStatus ret = BuildStatus.SUCCESS;

		RunProcess proc = new RunProcess(System.getenv("JAVA_HOME") + "/bin/java");
		File indir = files.getBaseDir();
		if (reldir != null)
			indir = new File(indir, reldir);
		proc.executeInDir(indir);
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		ThreadedStreamReader stdout = (ThreadedStreamReader) proc.captureStdout();
		ThreadedStreamReader stderr = (ThreadedStreamReader) proc.captureStderr();
		stdout.appendTo(new File(errdir, "stdout"));
		stderr.appendTo(new File(errdir, "stderr"));
		proc.arg("-classpath");
		FileUtils.createFile(new File(errdir, "javaclasspath"), classpath.toString());
		proc.arg(classpath.toString());
		for (String s : defines)
			proc.arg(s);
		proc.arg(mainClass);
		for (String s : args)
			proc.arg(s);
		proc.execute();
		int code = proc.getExitCode();
		if (code != 0)
		{
			System.out.println(proc.getStdout());
			System.out.println(proc.getStderr());
			System.out.println("Process exited with code " + code);
			return errorReturn; // Broken or TestFail, depending on user choice
		}
		return ret;
	}

	protected ByteCodeFile checkIfContainsTests(String qualifiedName, File clsFile) {
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
			File f2 = new File(bindir, FileUtils.ensureExtension(f, ".class").getPath());
			if (f2.exists() && checkIfContainsTests(FileUtils.convertToDottedName(f), f2) != null)
				return bcf;
		}
		return null;
	}

	public void addToClasspath(File resource) {
		classpath.add(resource);
	}

	public void addLibs(List<PendingResource> junitLibs) {
		if (junitLibs.isEmpty())
			return;
		
		for (PendingResource r : junitLibs)
			needs(r);
	}

	@Override
	public String identifier() {
		return "Java[" + mainClass + (reldir != null ? "-"+reldir:"") + (label != null ? "-"+label:"") + "]";
	}

	@Override
	public String toString() {
		return "Java " + mainClass + (reldir != null ? " in "+reldir:"") + (label != null ? " - " + label:"");
	}
	
	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public boolean onCascade() {
		return false;
	}
	
	@Override
	public boolean analyzeExports() {
		return true;
	}
}
