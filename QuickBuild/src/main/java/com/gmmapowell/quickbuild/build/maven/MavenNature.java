package com.gmmapowell.quickbuild.build.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ProxyableConnection;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

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
		mvnrepos.add("http://repo1.maven.org/maven2");
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
	public void resourceAvailable(BuildResource br) {
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
		File mavenToFile = FileUtils.mavenToFile(pkginfo);
		File cacheFile = new File(mvnCache, mavenToFile.getPath());
		if (!cacheFile.exists())
			downloadFromMaven(pkginfo, mavenToFile, cacheFile);
		MavenResource res = new MavenResource(pkginfo, cacheFile);
		config.resourceAvailable(res);
	}

	private void downloadFromMaven(String pkginfo, File mavenToFile, File cacheTo) {
		if (mvnrepos.size() == 0)
			throw new QuickBuildException("There are no maven repositories specified");
		for (String repo : mvnrepos)
		{
			ProxyableConnection conn = config.newConnection(FileUtils.urlPath(repo, mavenToFile));
			try {
				FileUtils.assertDirectory(cacheTo.getParentFile());
				FileOutputStream fos = new FileOutputStream(cacheTo);
				FileUtils.copyStream(conn.getInputStream(), fos);
				fos.close();
				System.out.println("Downloaded " + pkginfo + " from " + repo);
				return;
			} catch (IOException e) {
				System.out.println("Could not find " + pkginfo + " at " + repo + ":\n  " + e.getMessage());
			}
		}
		throw new QuickBuildException("Could not find maven package " + pkginfo);
	}

	@Override
	public void info(StringBuilder sb) {
		sb.append("    mvncache = " + mvnCache + "\n");
		for (String s : mvnrepos)
			sb.append("    repo: " + s + "\n");
		sb.append("    downloaded: " + loadedLibs + "\n");

	}	

}
