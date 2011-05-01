package com.gmmapowell.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;

public class FileUtils {

	public static class GlobFilter implements FileFilter {
		private final String pattern;
		private final Collection<File> includeOnlyDirs;
		private final Collection<File> excludeOnlyDirs;
		private final File rootdir;

		public GlobFilter(File file, String pattern, Collection<File> includeOnlyDirs, Collection<File> excludeOnlyDirs) {
			this.rootdir = file;
			this.pattern = pattern;
			this.includeOnlyDirs = includeOnlyDirs;
			this.excludeOnlyDirs = excludeOnlyDirs;
		}

		@Override
		public boolean accept(File f) {
			File relativeParent = makeRelativeTo(f.getParentFile(), rootdir);
			return StringUtil.globMatch(pattern, f.getName()) &&
			(includeOnlyDirs == null || includeOnlyDirs.contains(relativeParent)) &&
			(excludeOnlyDirs == null || !excludeOnlyDirs.contains(relativeParent));
		}
	}

	private static FileFilter isdirectory = new FileFilter() {
		@Override
		public boolean accept(File path) {
			return path.isDirectory();
		}
	};

	private static FileFilter anyFile = new FileFilter() {
		@Override
		public boolean accept(File dir) {
			return true;
		}
	};

	private static Comparator<? super File> filePathComparator = new Comparator<File>() {

		@Override
		public int compare(File o1, File o2) {
			if (o1.getPath().length() > o2.getPath().length())
				return -1;
			else if (o1.getPath().length() == o2.getPath().length())
				return 0;
			else
				return 1;
		}
	};
	
	private static File root = new File(System.getProperty("user.dir"));

	public static void chdirAbs(File absFile) {
		try
		{
			if (!absFile.isDirectory())
				throw new UtilException("Cannot have " + root + " be the root directory, because it does not exist");
			root = absFile;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}


	public static void chdir(File parentFile) {
		try
		{
			File changeTo = new File(root, parentFile.getPath()).getCanonicalFile();
			if (!changeTo.isDirectory())
				throw new UtilException("Cannot have " + root + " be the root directory, because it does not exist");
			root = changeTo;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static File relativePath(String string) {
		return new File(root, string);
	}
	
	public static File relativePath(File f) {
		if (f.isAbsolute())
			return f;
		else
			return new File(root, f.getPath());
	}
	
	public static File relativePath(File qbdir, String string) {
		if (qbdir == null)
			return new File(string);
		else if (qbdir.isAbsolute())
			return new File(qbdir, string);
		else
			return relativePath(new File(qbdir, string).getPath());
	}

	public static File makeRelative(File f) {
		return makeRelativeTo(f, root);
	}
	
	public static File makeRelativeTo(File f, File under) {
		if (under == null)
			return f;
		String uf = under.getPath();
		String tf = f.getPath();
		if (uf.equals(tf))
			return new File("");
		uf += File.separator;
		if (!tf.startsWith(uf))
			throw new RuntimeException("This case is not handled: " + tf + " is not a subdir of " + uf);
		return new File(tf.substring(uf.length()));
	}

	// TODO: this should consider all possible breakups based on -
	public static File findDirectoryNamed(String projectName) {
		File ret = new File(root, projectName);
		if (ret.isDirectory())
			return ret;
		if ((ret = findDirNamedRecursive(root, projectName)) != null)
			return ret;
		throw new UtilException("There is no project directory: " + projectName);
	}
	
	private static File findDirNamedRecursive(File base, String remaining) {
		System.out.println("Looking for " + remaining + " in " + base);
		int idx = -1;
		do
		{
			idx = remaining.indexOf("-", idx+1);
			File rec;
			if (idx == -1)
				rec = new File(base, remaining);
			else
				rec = new File(base, remaining.substring(0, idx));
			if (rec.isDirectory())
			{
				if (idx == -1)
					return rec;
				File ret = findDirNamedRecursive(rec, remaining.substring(idx+1));
				if (ret != null)
					return ret;
			}
		} while (idx != -1);
		return null;
	}

	// TODO: this feels very functional in its combinations of things
	public static List<File> findFilesMatchingIncluding(File dir, String string, List<File> includePackages) {
		return findFiles(dir, null, string, includePackages, null);
	}

	public static List<File> findFilesMatchingExcluding(File dir, String string, List<File> excludePackages) {
		return findFiles(dir, null, string, null, excludePackages);
	}

	public static List<File> findFilesMatching(File file, String string) {
		return findFiles(file, null, string, null, null);
	}

	public static List<File> findFilesUnderMatching(File file, String string) {
		return findFiles(file, file, string, null, null);
	}

	private static List<File> findFiles(File file, File under, String string, Collection<File> includeOnlyDirs, Collection<File> excludeOnlyDirs) {
		List<File> ret = new ArrayList<File>();
		if (!file.exists())
			throw new UtilException("There is no file " + file);
		FileFilter filter = new GlobFilter(file, string, includeOnlyDirs, excludeOnlyDirs);
		findRecursive(ret, filter, under, file);
		return ret;
	}

	public static List<File> findDirectoriesUnder(File dir) {
		List<File> ret = new ArrayList<File>();
		if (!dir.exists())
			throw new UtilException("There is no file " + dir);
		findRecursive(ret, isdirectory, dir, dir);
		return ret;
	}

	private static void findRecursive(List<File> ret, FileFilter filter, File under, File dir) {
		File[] contents = dir.listFiles(filter);
		if (contents == null)
			return;
		for (File f : contents)
			ret.add(makeRelativeTo(f, under));
		File[] subdirs = dir.listFiles(isdirectory);
		for (File d : subdirs)
			findRecursive(ret, filter, under, d);
	}

	public static String convertToDottedName(File path) {
		if (path.getParent() == null)
			return dropExtension(path.getName());
		return convertToDottedName(path.getParentFile()) + "." + path.getName();
	}

	public static String convertToDottedNameDroppingExtension(File path) {
		if (path.getParent() == null)
			return dropExtension(path.getName());
		return convertToDottedName(path.getParentFile()) + "." + dropExtension(path.getName());
	}

	public static String dropExtension(String name) {
		int idx = name.indexOf('.');
		if (idx == -1)
			return name;
		return name.substring(0, idx);
	}

	public static File mavenToFile(String pkginfo) {
		String[] spl = pkginfo.split(":");
		if (spl == null || spl.length != 4)
			throw new UtilException("'" + pkginfo + "' is not a valid maven package name");
		return fileConcat(convertDottedToPath(spl[0]).getPath(), spl[1], spl[3], spl[1]+"-"+spl[3]+"."+spl[2]);
	}

	public static File convertDottedToPath(String pkg) {
		String[] spl = pkg.split("\\.");
		return fileConcat(spl);
	}
	
	public static String convertDottedToSlashPath(String pkg) {
		String[] spl = pkg.split("\\.");
		String ret = "";
		for (String s : spl)
			ret += "/" + s;
		return ret.substring(1);
	}

	public static File fileConcat(String... spl) {
		File ret = null;
		for (String s : spl)
		{
			if (s == null)
				continue;
			if (ret == null)
				ret = new File(s);
			else
				ret = new File(ret, s);
		}
		if (ret == null)
			throw new UtilException("Could not concatenate " + Arrays.toString(spl));
		return ret;
	}

	public static String urlPath(String root, File mavenToFile) {
		if (mavenToFile == null)
		{
			if (root.endsWith("/"))
				return root.substring(0, root.length()-1);
			return root;
		}
		return urlPath(root, mavenToFile.getParentFile()) + "/" + mavenToFile.getName();
	}

	public static void copyStream(InputStream inputStream, FileOutputStream fos) throws IOException {
		byte[] bs = new byte[500];
		int cnt = 0;
		while ((cnt = inputStream.read(bs, 0, 500)) > 0)
			fos.write(bs, 0, cnt);
	}

	public static void assertDirectory(File file) {
		if (!file.exists())
			if (!file.mkdirs())
				throw new UtilException("Cannot create directory " + file);
		if (!file.isDirectory())
			throw new UtilException("Maven cache directory '" + file + "' is not a directory");
	}

	public static File getCurrentDir() {
		return root;
	}

	public static void cleanDirectory(File dir) {
		List<File> ret = new ArrayList<File>();
		findRecursive(ret, anyFile, dir, dir);
		// sort longest to shortest to resolve empty directories
		Collections.sort(ret, filePathComparator);
		for (File f : ret)
		{
			if (!new File(dir, f.getPath()).delete())
				throw new UtilException("Could not delete: " + f);
		}
	}

	public static File combine(File path1, String path2) {
		if (path1 == null && path2 == null)
			return null;
		else if (path1 == null)
			return new File(path2);
		else if (path2 == null)
			return path1;
		else
			return new File(path1, path2);
	}

	public static File combine(File path1, File path2) {
		if (path1 == null && path2 == null)
			return null;
		else if (path1 == null)
			return path2;
		else if (path2 == null)
			return path1;
		else
			return new File(path1, path2.getPath());
	}

	public static String getHostName() {
		try {
		    return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw UtilException.wrap(e);
		}
	}

	public static void cat(File file) {
		try {
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String s;
			while ((s = lnr.readLine()) != null)
				System.out.println(s);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public static String getUnextendedName(File file) {
		String ret = file.getName();
		if (ret.indexOf(".") == -1)
			return ret;
		return ret.substring(0, ret.indexOf("."));
	}

	public static String getPackage(File file) {
		return convertToDottedName(file.getParentFile());
	}

	public static void copyRecursive(File from, File to) {
		if (from == null)
			return;
		assertDirectory(to);
		File[] toCopy = from.listFiles();
		int nerrors = 0;
		for (File f : toCopy)
		{
			try {
				File f2 = new File(to, f.getName());
				if (f.isDirectory())
					copyRecursive(f, f2);
				else
					copy(f, f2);
			} catch (Exception ex) {
				nerrors++;
			}
		}
		if (nerrors > 0)
			throw new UtilException("Encountered " + nerrors + " copying " + from + " to " + to);
	}

	private static void copy(File f, File f2) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		FileOutputStream fos = new FileOutputStream(f2);
		copyStream(fis, fos);
		fis.close();
		fos.close();
	}

	public static String clean(String name) {
		StringBuilder ret = new StringBuilder(name);
		for (int i=0;i<ret.length();i++)
		{
			char c = ret.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '.' && c != '_')
				ret.setCharAt(i, '.');
		}
		for (int i=ret.length()-1;i>=0 && ret.charAt(i ) == '.';i--)
			ret.deleteCharAt(i);
		return ret.toString();
	}

	public static Set<File> directorySet(Iterable<File> sourceFiles) {
		HashSet<File> ret = new HashSet<File>();
		for (File f : sourceFiles)
			ret.add(f.getParentFile());
		return ret;
	}
}
