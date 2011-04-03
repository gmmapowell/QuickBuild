package com.gmmapowell.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.FuncR1;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;

public class ClassPathResource {
	private final File file;
	private final GPJarEntry jarEntry;
	private final File under;

	public ClassPathResource(File under, File file) {
		this.under = under;
		this.file = file;
		this.jarEntry = null;
	}

	public ClassPathResource(GPJarEntry jarEntry) {
		this.under = null;
		this.file = null;
		this.jarEntry = jarEntry;
	}


	public static FuncR1<ClassPathResource, File> fromFile(final File from) {
		return new FuncR1<ClassPathResource, File>() {
			@Override
			public ClassPathResource apply(File arg1) {
				return new ClassPathResource(from, arg1);
			}
		};
	};
	
	public static FuncR1<ClassPathResource, GPJarEntry> fromJar = new FuncR1<ClassPathResource, GPJarEntry>() {

		@Override
		public ClassPathResource apply(GPJarEntry arg1) {
			return new ClassPathResource(arg1);
		}
	};

	public String toString()
	{
		if (file != null)
			return file.toString();
		else
			return jarEntry.toString();
	}

	public InputStream asStream() {
		try {
			if (file != null)
				return new FileInputStream(file);
			else
				return jarEntry.asStream();
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	public long length() {
		if (file != null)
			return file.length();
		else
			return jarEntry.length();
	}

	public String getName() {
		if (file != null)
			return file.getPath();
		else
			return jarEntry.getName();
	}

	public String getRelativeName() {
		if (file != null)
			return FileUtils.makeRelativeTo(file, under).getPath();
		return jarEntry.getName();
	}
}