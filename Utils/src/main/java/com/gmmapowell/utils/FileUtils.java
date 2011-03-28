package com.gmmapowell.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class FileUtils {

	public static class GlobFilter implements FileFilter {
		private final String pattern;

		public GlobFilter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean accept(File f) {
			return StringUtil.globMatch(pattern, f.getName());
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


	public static void chdir(File parentFile) {
		try
		{
			root = new File(root, parentFile.getPath()).getCanonicalFile();
			if (!root.isDirectory())
				throw new UtilException("Cannot have " + root + " be the root directory, because it does not exist");
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

	public static File relativeTo(File f) {
		return relativeTo(f, root);
	}
	
	public static File relativeTo(File f, File under) {
		if (under == null)
			return f;
		String uf = under.getPath()+File.separator;
		String tf = f.getPath();
		if (!tf.startsWith(uf))
			throw new RuntimeException("This case is not handled");
		return new File(tf.substring(uf.length()));
	}

	// TODO: this should consider all possible breakups based on -
	public static File findDirectoryNamed(String projectName) {
		File ret = new File(root, projectName);
		if (ret.isDirectory())
			return ret;
		throw new UtilException("There is no project directory: " + projectName);
	}
	
	public static List<File> findFilesMatching(File file, String string) {
		return findFiles(file, null, string);
	}

	public static List<File> findFilesUnderMatching(File file, String string) {
		return findFiles(file, file, string);
	}

	private static List<File> findFiles(File file, File under, String string) {
		List<File> ret = new ArrayList<File>();
		if (!file.exists())
			throw new UtilException("There is no file " + file);
		FileFilter filter = new GlobFilter(string);
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
			ret.add(relativeTo(f, under));
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

	private static String dropExtension(String name) {
		int idx = name.indexOf('.');
		if (idx == -1)
			return name;
		return name.substring(0, idx);
	}

	public static File mavenToFile(String pkginfo) {
		String[] spl = pkginfo.split(":");
		if (spl == null || spl.length != 4)
			throw new UtilException("'" + pkginfo + "' is not a valid maven package name");
		return fileConcat(convertPackageToPath(spl[0]).getPath(), spl[1], spl[3], spl[1]+"-"+spl[3]+"."+spl[2]);
	}

	private static File convertPackageToPath(String pkg) {
		String[] spl = pkg.split("\\.");
		return fileConcat(spl);
	}

	private static File fileConcat(String... spl) {
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
}
