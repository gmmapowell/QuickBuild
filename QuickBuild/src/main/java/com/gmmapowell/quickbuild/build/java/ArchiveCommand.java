package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.Strategem;

import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public abstract class ArchiveCommand extends AbstractTactic {
	protected File jarfile;
	protected JarResource jarResource;
	protected final List<File> dirsToJar = new ArrayList<File>();
	protected final List<File> includePackages;
	protected final List<File> excludePackages;
	protected final OrderedFileList resourceFiles;
	private final MainClassCommand mainClass;
	private List<ManifestClassPathCommand> classPaths;

	public ArchiveCommand(Strategem parent, List<File> includePackages, List<File> excludePackages, OrderedFileList resourceFiles, MainClassCommand mainClass, List<ManifestClassPathCommand> classPaths) {
		super(parent);
		this.includePackages = includePackages;
		this.excludePackages = excludePackages;
		this.resourceFiles = resourceFiles;
		this.mainClass = mainClass;
		this.classPaths = classPaths;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return resourceFiles;
	}

	public void add(File file) {
		dirsToJar.add(file);
	}

	public File getFile() {
		return FileUtils.makeRelative(jarfile);
	}

	public JarResource getJarResource() {
		return jarResource;
	}

	public boolean alwaysBuild() {
		return false;
	}
	
	protected void writeManifest(JarOutputStream jos) throws IOException {
		jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
		PrintWriter pw = new PrintWriter(jos);
		if (mainClass != null)
			pw.println("Main-Class: " + mainClass.getName());
		if (!classPaths.isEmpty()) {
			pw.print("Class-Path:");
			for (ManifestClassPathCommand c : classPaths) {
				pw.print(" " + c.getName());
			}
			pw.println();
		}
		pw.flush();
	}
}
