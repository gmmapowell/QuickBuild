package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;

public class DexBuildCommand extends AbstractTactic {
	private final AndroidContext acxt;
	private final File bindir;
	private final File dexFile;
	private final List<File> jars = new ArrayList<File>();
	private final File libdir;
	private final Set<Pattern> exclusions;
	private final File tmpLib;
	private final File rawapkLib;
	private ResourcePacket<PendingResource> uselibs;
	private List<String> restrictTo = null;

	public DexBuildCommand(AndroidContext acxt, Strategem parent, StructureHelper files, File bindir, File libdir, File dexFile, Set<Pattern> exclusions, ResourcePacket<PendingResource> uselibs) {
		super(parent);
		this.acxt = acxt;
		this.bindir = bindir;
		this.libdir = libdir;
		this.dexFile = dexFile;
		this.exclusions = exclusions;
		this.uselibs = uselibs;
		System.out.println("libdir = " + libdir);
		this.tmpLib = files.getRelative("libs");
		this.rawapkLib = files.getRelative("src/android/rawapk/lib");
	}

	public void addJar(File file) {
		if (file.getName().endsWith(".aar")) {
//			System.out.println("Unpacking " + file);
			// unpack aar into jars in libdir and raw assets in rawapk
			JarFile jf = null;
			try {
				jf = new JarFile(file);
				Enumeration<JarEntry> entries = jf.entries();
				int k = 1;
				while (entries.hasMoreElements()) {
					JarEntry e = entries.nextElement();
					String n = e.getName();
					if (n.endsWith(".so")) {
//						System.out.println("so: " + n);
						if (!n.startsWith("jni/"))
							throw new UtilException("so not under jni/");
						n = n.substring(4);
						boolean doit = true;
						if (restrictTo != null) {
							doit = false;
							for (String s : restrictTo) {
								if (n.startsWith(s+"/"))
									doit = true;
							}
						}
						if (doit) {
							File writeTo = new File(rawapkLib, n);
							FileUtils.assertDirectory(writeTo.getParentFile());
							FileUtils.copyStreamToFile(jf.getInputStream(e), writeTo);
//							System.out.println("Writing so to " + writeTo);
						}
					} else if (n.endsWith(".jar")) {
						while (true) {
							File foo = new File(tmpLib, FileUtils.ensureExtension(file, "-"+k+".jar").getName());
//							System.out.println("jar trying: " + foo);
							if (!foo.exists()) {
								FileUtils.copyStreamToFile(jf.getInputStream(e), foo);
								jars.add(foo);
								break;
							}
							k++;
						}
					}
				}
			} catch (Throwable t) {
				throw UtilException.wrap(t);
			} finally {
				if (jf != null)
					try {
						jf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		} else
			jars.add(file);
	}

	public void restrictArch(List<String> restrictTo) {
		this.restrictTo = restrictTo;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunProcess proc = new RunProcess(acxt.getDX().getPath());
		proc.captureStdout();
		proc.captureStderr();
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		
		if (uselibs != null)
		{
			FileUtils.cleanDirectory(tmpLib);
			for (PendingResource pr : uselibs)
			{
				File path = pr.physicalResource().getPath();
				addJar(path);
			}
			uselibs = null;
		}
		
		proc.arg("--dex");
		proc.arg("--output="+dexFile.getPath());
		proc.arg(bindir.getPath());
		
		LinkedHashSet<String> paths = new LinkedHashSet<String>();
		considerAdding(paths, acxt.getSupportJar().getPath());
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource)
				considerAdding(paths, br.getPath().getPath());
		}
		
		for (File f : FileUtils.findFilesMatching(libdir, "*.jar"))
			considerAdding(paths, f.getPath());
		for (File f : jars)
			considerAdding(paths, f.getPath());

		for (String s : paths)
			proc.arg(s);
		
		proc.execute();
		if (proc.getStderr().length() > 0 || proc.getStdout().length() > 0)
		{
			System.out.println(proc.getStdout());
			System.out.println(proc.getStderr());
			return BuildStatus.BROKEN;
		}
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}
	
	private void considerAdding(LinkedHashSet<String> paths, String path) {
		for (Pattern patt : exclusions)
			if (patt.matcher(path.toLowerCase().replaceAll("\\\\", "/")).find())
				return;
		paths.add(path);
	}

	@Override
	public String toString() {
		return "Create Dex: " + dexFile;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "dex");
	}
}
