package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.JarBuildCommand;
import com.gmmapowell.quickbuild.build.JavaBuildCommand;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File projectDir;
	private Project project;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void applyConfig(Config config) {
		project = new Project("jar", projectName, projectDir, config.getOutput());
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public Project project() {
		return project;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("jar " + projectName);
		return sb.toString();
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		JarBuildCommand jar = new JarBuildCommand(project, project.getName() + ".jar");
		List<BuildCommand> ret = new ArrayList<BuildCommand>();
		addJavaBuild(ret, jar, "src/main/java", "classes");
		JavaBuildCommand junit = addJavaBuild(ret, null, "src/test/java", "test-classes");
		if (junit != null)
			junit.addToClasspath(new File(project.getOutputDir(), "classes"));
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		addJUnitRun(ret, junit);
		if (ret.size() == 0)
			throw new QuickBuildException("None of the required source directories exist");
		ret.add(jar);
		return ret;
	}

	private JavaBuildCommand addJavaBuild(List<BuildCommand> accum, JarBuildCommand jar, String src, String bin) {
		if (new File(projectDir, src).isDirectory())
		{
			if (jar != null)
				jar.add(new File(project.getOutputDir(), bin));
			JavaBuildCommand ret = new JavaBuildCommand(project, src, bin);
			accum.add(ret);
			return ret;
		}
		return null;
	}
	
	private void addResources(JarBuildCommand jar, JavaBuildCommand junit, String src) {
		File dir = new File(projectDir, src);
		if (dir.isDirectory())
		{
			if (jar != null)
				jar.add(dir);
			if (junit != null)
				junit.addToClasspath(dir);
		}
	}

	private void addJUnitRun(List<BuildCommand> ret, JavaBuildCommand jbc) {
		if (jbc != null)
		{
			ret.add(new JUnitRunCommand(project, jbc));
		}
	}

}
