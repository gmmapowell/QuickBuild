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

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.ZUJarEntry;
import org.zinutils.utils.ZUJarFile;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.BuildIfCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.DirectoryResourceCommand;
import com.gmmapowell.quickbuild.config.FileListCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategemTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;

public class JarJarCommand extends AbstractStrategemTactic {
	private String outputTo;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final List<Object> resources = new ArrayList<Object>();
	private final List<BuildIfCommand> buildifs = new ArrayList<>();
	private final List<FileListCommand> filelists = new ArrayList<>();
	private MainClassCommand mainClass;
	private GitIdCommand gitIdCommand;

	public JarJarCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "outputTo", "output file"));
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
		builds.add(new JarResource(this, FileUtils.relativePath(outputTo)));
		for (ConfigApplyCommand opt : options)
		{
			opt.applyTo(config);
			if (opt instanceof ResourceCommand)
			{
				addResource((ResourceCommand)opt);
			}
			else if (opt instanceof DirectoryResourceCommand)
			{
				resources.add(opt);
			}
			else if (opt instanceof MainClassCommand)
			{
				if (mainClass != null)
					throw new UtilException("You cannot specify more than one main class");
				mainClass = (MainClassCommand) opt;
			}
			else if (opt instanceof GitIdCommand)
			{
				if (gitIdCommand != null)
					throw new UtilException("You cannot specify more than one git id variable");
				gitIdCommand = (GitIdCommand) opt;
			}
			else if (opt instanceof BuildIfCommand)
			{
				buildifs.add((BuildIfCommand) opt);
			}
			else if (opt instanceof FileListCommand)
			{
				filelists.add((FileListCommand) opt);
			}
			else
				throw new UtilException("The option " + opt + " is not valid for JarJarCommand");
		}
		return this;
	}

	private void addResource(ResourceCommand opt) {
		needs.add(opt.getPendingResource());
		resources.add(opt);
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
	public OrderedFileList sourceFiles() {
		OrderedFileList ret = new OrderedFileList();
		for (FileListCommand fl : filelists) {
			fl.addToOFL(ret);
		}
		return ret;
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JarOutputStream jos = null;
		try {
			File of = new File(outputTo).getParentFile();
			if (of == null)
				of = rootDirectory();
			FileUtils.assertDirectory(of);
			jos = new JarOutputStream(new FileOutputStream(FileUtils.relativePath(outputTo)));
			Set<String> entries = new HashSet<String>();
			writeManifest(jos);
			if (gitIdCommand != null) {
				gitIdCommand.writeTrackerFile(cxt, jos, "META-INF", identifier());
			}
				
			for (Object rc : resources)
			{
				if (rc instanceof ResourceCommand) {
					PendingResource pr = ((ResourceCommand) rc).getPendingResource();
					BuildResource actual = pr.physicalResource();
					if (!(actual instanceof JarResource))
						throw new UtilException(pr + " is not a jar resource");
					if (showDebug)
						System.out.println("Considering resource " + actual.getPath());
					try (ZUJarFile gpj = new ZUJarFile(actual.getPath())) {
						for (ZUJarEntry je : gpj)
						{
							String name = je.getName();
							if (showDebug)
								System.out.println("  Looking at path " + name);
							if (name.equals("META-INF/"))
								continue;
							else if (name.equals("META-INF/MANIFEST.MF"))
								continue;
							else if (name.endsWith("LICENSE.txt"))
								continue;
							else if (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA"))
								continue;
							else if (name.startsWith(".git"))
								continue;
							else if (name.endsWith("/") && entries.contains(name))
								continue;
							else if (name.startsWith("META-INF/") && entries.contains(name))
								continue;
							else if (!((ResourceCommand) rc).includes(name))
							{
								if (showDebug)
									System.out.println("    not included");
								continue;
							}
							if (showDebug)
								System.out.println("    adding as " + je.getJava());
							jos.putNextEntry(new JarEntry(je.getJava()));
							FileUtils.copyStream(je.asStream(), jos);
							entries.add(name);
						}
					}
				} else if (rc instanceof DirectoryResourceCommand) {
					DirectoryResourceCommand drc = (DirectoryResourceCommand) rc;
					for (File f : FileUtils.findFilesMatching(drc.rootDir, "*")) {
						if (f.isDirectory())
							continue;
						String name = FileUtils.makeRelativeTo(f, drc.rootDir).getPath();
						if (drc.includes(name)) {
							String prfName = drc.prefix(name);
							jos.putNextEntry(new JarEntry(prfName));
							FileUtils.copyFileToStream(FileUtils.relativePath(drc.rootDir, name), jos);
							entries.add(prfName);
						}
					}
				} else
					throw new UtilException("Cannot handle " + rc);
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
}
