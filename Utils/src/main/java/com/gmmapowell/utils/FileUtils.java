package com.gmmapowell.utils;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;

public class FileUtils {

	// TODO: this should consider all possible breakups based on -
	public static File findDirectoryNamed(String projectName) {
		File ret = new File(projectName);
		if (ret.isDirectory())
			return ret;
		throw new UtilException("There is no project directory: " + projectName);
	}

}
