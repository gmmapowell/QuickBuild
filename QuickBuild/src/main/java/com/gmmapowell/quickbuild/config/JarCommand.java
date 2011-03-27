package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildClassPath;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.JarBuildCommand;
import com.gmmapowell.quickbuild.build.JavaBuildCommand;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand {
	private List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private File projectDir;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  jar " + projectName + "\n");
		return sb.toString();
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands(Config conf) {
		JarBuildCommand jar = new JarBuildCommand(projectDir);
		List<BuildCommand> ret = new ArrayList<BuildCommand>();
		BuildClassPath bcp = new BuildClassPath();
		addJavaBuild(conf, ret, jar, bcp, "src/main/java", "classes");
		addJavaBuild(conf, ret, null, bcp, "src/test/java", "test-classes");
		addResources(jar, bcp, "src/main/resources");
		addResources(null, bcp, "src/main/resources");
		if (ret.size() == 0)
			throw new QuickBuildException("None of the required source directories exist");
		ret.add(jar);
		return ret;
	}

	private void addJavaBuild(Config conf, List<BuildCommand> ret, JarBuildCommand jar, BuildClassPath bcp, String src, String bin) {
		if (new File(projectDir, src).isDirectory())
		{
			bcp.add(new File(projectDir, bin));
			if (jar != null)
				jar.add(new File(projectDir, bin));
			ret.add(new JavaBuildCommand(conf, projectDir, src, bin));
		}
	}
	
	private void addResources(JarBuildCommand jar, BuildClassPath bcp, String src) {
		if (new File(projectDir, src).isDirectory())
		{
			if (jar != null)
				jar.add(new File(projectDir, src));
			bcp.add(new File(projectDir, src));
		}
	}
}
