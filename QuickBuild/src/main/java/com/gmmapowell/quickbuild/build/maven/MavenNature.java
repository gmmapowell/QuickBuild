package com.gmmapowell.quickbuild.build.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ProxyableConnection;

public class MavenNature implements Nature {
	private final List<String> mvnrepos = new ArrayList<String>();
	private final List<String> loadedLibs = new ArrayList<String>();
	private File mvnCache;
	private final Config config;

	// Init is called on registration
	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("repo", RepoCommand.class);
		config.addCommandExtension("maven", MavenLibraryCommand.class);
	}
	
	// The constructor is called the _first_ time someone uses it
	public MavenNature(Config config)
	{
		this.config = config;
		mvnrepos.add("https://repo1.maven.org/maven2");
		mvnCache = FileUtils.relativePath(config.getQuickBuildDir(), "mvncache");
		if (!mvnCache.exists())
			if (!mvnCache.mkdirs())
				throw new QuickBuildException("Cannot create directory " + mvnCache);
		if (!mvnCache.isDirectory())
			throw new QuickBuildException("Maven cache directory '" + mvnCache + "' is not a directory");
	}

	public void clearMavenRepos() {
		mvnrepos.clear();
	}
	
	public void addMavenRepo(String repo) {
		mvnrepos.add(repo);
	}

	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
		throw new UtilException("Can't handle " + br);
	}

	public boolean isAvailable() {
		return true;
	}

	@Override
	public void done() {
	}
	
	public void loadPackage(String pkginfo) {
		loadedLibs.add(pkginfo);
		File mavenPath = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenPath.getPath());
		File jarFile = null;
		if (cacheFile.getName().endsWith(".aar")) {
			jarFile = new File(cacheFile.getParentFile(), cacheFile.getName().replace(".aar", ".jar"));
		}
		if (!cacheFile.exists() || (jarFile != null && !jarFile.exists()))
			downloadFromMaven(pkginfo, mavenPath, cacheFile, jarFile, "");
		if (jarFile == null) {
			File mavenSource = makeSourcesPath(mavenPath);
			File sourceFile = new File(mvnCache, mavenSource.getPath());
			if (!sourceFile.exists())
				downloadFromMaven(pkginfo, mavenSource, sourceFile, null, " sources");
		}
		MavenResource res = new MavenResource(pkginfo, jarFile != null ? jarFile : cacheFile);
		config.resourceAvailable(res);
	}

	private void downloadFromMaven(String pkginfo, File mavenPath, File cacheTo, File extractTo, String category) {
		if (mvnrepos.size() == 0)
			throw new QuickBuildException("There are no maven repositories specified");
		List<String> pathsTried = new ArrayList<String>();
		for (String repo : mvnrepos)
		{
			String urlPath = FileUtils.urlPath(repo, mavenPath);
			pathsTried.add(urlPath);
			try {
				doDownload(urlPath, cacheTo);
				System.out.println("Downloaded " + pkginfo + category + " from " + repo);
				if (cacheTo.getName().endsWith(".aar")) {
					JarFile jf = new JarFile(cacheTo);
					try {
						JarEntry je = jf.getJarEntry("classes.jar");
						InputStream is = jf.getInputStream(je);
						FileUtils.copyStreamToFile(is, extractTo);
						System.out.println("  Extracted classes.jar to " + extractTo);
					} finally {
						jf.close();
					}
				}
				return;
			} catch (IOException e) {
				cacheTo.delete();
				if (trySnapshot(repo, pkginfo, mavenPath, cacheTo))
					return;
//				System.out.println("Could not find " + pkginfo + " at " + repo + ":\n  " + e.getMessage());
			}
		}
		throw new QuickBuildException("Could not find maven package " + pkginfo + " at any of " + pathsTried);
	}

	private File makeSourcesPath(File mavenPath) {
		String name = FileUtils.dropExtension(mavenPath.getName()) + "-sources.jar";
		return new File(mavenPath.getParentFile(), name);
	}

	private void doDownload(String urlPath, File cacheTo) throws FileNotFoundException, IOException {
		ProxyableConnection conn = config.newConnection(urlPath);
		FileUtils.assertDirectory(cacheTo.getParentFile());
		FileOutputStream fos = new FileOutputStream(cacheTo);
		FileUtils.copyStream(conn.getInputStream(), fos);
		fos.close();
	}

	/** It seems that there are times when snapshots can have random time/date fields in lieu of "SNAPSHOT" in the
	 * actual version.  Handle this case.
	 * @return 
	 */
	private boolean trySnapshot(String repo, String pkginfo, File mavenToFile, File cacheTo) {
		try {
			File pf = mavenToFile.getParentFile();
			String url = FileUtils.urlPath(repo, pf) +"/";
			ProxyableConnection conn = config.newConnection(url);
			ByteArrayOutputStream sw = new ByteArrayOutputStream();
			FileUtils.copyStream(conn.getInputStream(), sw);
			String contents = new String(sw.toByteArray());

			String patt = "\"([^\"']*" + mavenToFile.getName().replace("SNAPSHOT", "[^\"']*") + ")\"";
			Pattern p = Pattern.compile(patt);
			Matcher matcher = p.matcher(contents);
			String shortest = null;
			while (matcher.find()) {
				if (shortest == null || shortest.length() > matcher.group(1).length())
					shortest = matcher.group(1);
			}
			if (shortest != null) {
				doDownload(shortest, cacheTo);
				return true;
			}
		}
		catch (IOException e) {
//			System.out.println("Exception: " + e.getMessage());
			cacheTo.delete();
		}
		return false;
	}

	@Override
	public void info(StringBuilder sb) {
		sb.append("    mvncache = " + mvnCache + "\n");
		for (String s : mvnrepos)
			sb.append("    repo: " + s + "\n");
		sb.append("    downloaded: " + loadedLibs + "\n");

	}	

}
