package com.gmmapowell.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class FileUtils {

	public static class GlobFilter implements FilenameFilter {
		private final String pattern;

		public GlobFilter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean accept(File dir, String name) {
			return StringUtil.globMatch(pattern, name);
		}
	}

	private static FileFilter isdirectory = new FileFilter() {
		@Override
		public boolean accept(File path) {
			return path.isDirectory();
		}
	};

	private static File root = new File(System.getProperty("user.dir"));

	public static void chdir(File parentFile) {
		try
		{
			root = new File(root, parentFile.getPath()).getCanonicalFile();
			if (!root.isDirectory())
				throw new UtilException("Cannot have " + root + " be the root directory, because it does not exist");
			System.out.println("Root directory is now: " + root);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
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
		FilenameFilter filter = new GlobFilter(string);
		findRecursive(ret, filter, under, file);
		return ret;
	}

	private static void findRecursive(List<File> ret, FilenameFilter filter, File under, File dir) {
		File[] contents = dir.listFiles(filter);
		if (contents == null)
			return;
		for (File f : contents)
			ret.add(relativeTo(f, under));
		File[] subdirs = dir.listFiles(isdirectory);
		for (File d : subdirs)
			findRecursive(ret, filter, under, d);
	}

	private static File relativeTo(File f, File under) {
		if (under == null)
			return f;
		String uf = under.getPath()+File.separator;
		String tf = f.getPath();
		if (!tf.startsWith(uf))
			throw new RuntimeException("This case is not handled");
		return new File(tf.substring(uf.length()));
	}

	public static String convertToPackageName(File path) {
		if (path.getParent() == null)
			return dropExtension(path.getName());
		return convertToPackageName(path.getParentFile()) + "." + dropExtension(path.getName());
	}

	private static String dropExtension(String name) {
		int idx = name.indexOf('.');
		if (idx == -1)
			return name;
		return name; //.substring(0, idx);
	}
}
