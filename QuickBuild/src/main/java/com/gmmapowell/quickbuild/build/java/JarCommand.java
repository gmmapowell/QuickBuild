package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final ResourcePacket sources = new ResourcePacket();
	private String projectName;
	private final File rootdir;
	private StructureHelper files;
	private String targetName;
	private JarResource jarResource;
	private List<Tactic> tactics;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootdir, config.getOutput());
		targetName = projectName + ".jar";
		jarResource = new JarResource(this, files.getOutput(targetName));

		JarBuildCommand jar = new JarBuildCommand(this, files, targetName);
		tactics = new ArrayList<Tactic>();
		addJavaBuild(tactics, jar, "src/main/java", "classes");
		JavaBuildCommand junit = addJavaBuild(tactics, null, "src/test/java", "test-classes");
		if (junit != null)
			junit.addToClasspath(new File(files.getOutputDir(), "classes"));
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		addJUnitRun(tactics, junit);
		if (tactics.size() == 0)
			throw new QuickBuildException("None of the required source directories exist");
		tactics.add(jar);
		
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("jar " + projectName);
		return sb.toString();
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		return tactics;
	}

	private JavaBuildCommand addJavaBuild(List<Tactic> accum, JarBuildCommand jar, String src, String bin) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			if (jar != null)
				jar.add(new File(files.getOutputDir(), bin));
			
			sources.add(new JavaSourceDirResource(this, dir));
			JavaBuildCommand ret = new JavaBuildCommand(this, files, src, bin);
			accum.add(ret);
			return ret;
		}
		return null;
	}
	
	private void addResources(JarBuildCommand jar, JavaBuildCommand junit, String src) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			if (jar != null)
				jar.add(dir);
			if (junit != null)
				junit.addToClasspath(dir);
		}
	}

	private void addJUnitRun(List<Tactic> ret, JavaBuildCommand jbc) {
		if (jbc != null)
		{
			ret.add(new JUnitRunCommand(this, files, jbc));
		}
	}

	// Certainly the idea is that this is the "static" resouces this guy needs
	// Dynamic resources come in some other way
	@Override
	public ResourcePacket needsResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket providesResources() {
		return sources;
	}

	@Override
	public ResourcePacket buildsResources() {
		ResourcePacket ret = new ResourcePacket();
		ret.add(jarResource);
		return ret;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

}
