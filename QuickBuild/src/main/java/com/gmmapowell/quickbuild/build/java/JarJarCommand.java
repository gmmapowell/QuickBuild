package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;
import com.gmmapowell.utils.OrderedFileList;

public class JarJarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic{

	private String outputTo;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private final List<Tactic> tactics = new ArrayList<Tactic>();
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final List<ResourceCommand> resources = new ArrayList<ResourceCommand>();
	private MainClassCommand mainClass;

	@SuppressWarnings("unchecked")
	public JarJarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "outputTo", "output file"));
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String identifier() {
		return "JarJar[" + outputTo + "]";
	}

	@Override
	public Strategem applyConfig(Config config) {
		tactics.add(this);
		builds.add(new JarResource(this, FileUtils.relativePath(outputTo)));
		for (ConfigApplyCommand opt : options)
		{
			opt.applyTo(config);
			if (opt instanceof ResourceCommand)
			{
				addResource((ResourceCommand)opt);
			}
			else if (opt instanceof MainClassCommand)
			{
				if (mainClass != null)
					throw new UtilException("You cannot specify more than one main class");
				mainClass = (MainClassCommand) opt;
			}
			else
				throw new UtilException("The option " + opt + " is not valid for JarCommand");
		}
		return this;
	}

	private void addResource(ResourceCommand opt) {
		needs.add(opt.getPendingResource());
		resources .add(opt);
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return provides;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return FileUtils.getCurrentDir();
	}

	@Override
	public List<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList();
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JarOutputStream jos = null;
		try {
			FileUtils.assertDirectory(new File(outputTo).getParentFile());
			jos = new JarOutputStream(new FileOutputStream(FileUtils.relativePath(outputTo)));
			// TODO: should write META-INF/MANIFEST.MF
			Set<String> entries = new HashSet<String>();
			writeManifest(jos);
			for (ResourceCommand rc : resources)
			{
				PendingResource pr = rc.getPendingResource();
				BuildResource actual = pr.physicalResource();
				if (!(actual instanceof JarResource))
					throw new UtilException(pr + " is not a jar resource");
				GPJarFile gpj = new GPJarFile(actual.getPath());
				for (GPJarEntry je : gpj)
				{
					String name = je.getName();
					if (name.equals("META-INF/"))
						continue;
					else if (name.equals("META-INF/MANIFEST.MF"))
						continue;
					else if (name.startsWith(".git"))
						continue;
					else if (name.endsWith("/") && entries.contains(name))
						continue;
					else if (name.startsWith("META-INF/") && entries.contains(name))
						continue;
					else if (!rc.includes(name))
						continue;
					jos.putNextEntry(new JarEntry(je.getJava()));
					FileUtils.copyStream(je.asStream(), jos);
					entries.add(name);
				}
			}
			jos.close();
			jos = null;
			builds.provide(cxt, false);
			return BuildStatus.SUCCESS;
		} catch (Exception ex) {
			ex.printStackTrace();
			cxt.failure(null, null, null);
			return BuildStatus.BROKEN;
		}
		finally {
			try {
				if (jos != null)
					jos.close();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}

	}

	private void writeManifest(JarOutputStream jos) throws IOException {
		jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
		PrintWriter pw = new PrintWriter(jos);
		if (mainClass != null)
			pw.println("Main-Class: " + mainClass.getName());
		pw.flush();
	}

	@Override
	public String toString() {
		return identifier();
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}
}
