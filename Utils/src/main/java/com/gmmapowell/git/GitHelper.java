package com.gmmapowell.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;

import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class GitHelper {

	public static boolean checkFiles(boolean b, OrderedFileList files, File file) {
		RunProcess proc = new RunProcess("git");
		proc.executeInDir(FileUtils.getCurrentDir());
		proc.captureStdout();
		proc.arg("hash-object");
		for (File f : files)
		{
			proc.arg(FileUtils.makeRelative(f).getPath());
		}
		proc.execute();
		boolean dirty = false;
		try
		{
			String out = proc.getStdout();
			LineNumberReader r = new LineNumberReader(new StringReader(out));
			FileOutputStream fos = new FileOutputStream(file);
			PrintWriter pw = new PrintWriter(fos);
			for (File f : files)
			{
				String s = r.readLine();
				if (s == null)
				{
					dirty = true;
				}
				pw.println(s + " " + FileUtils.makeRelative(f).getPath());
			}
			pw.close();
			fos.close();
		}
		catch (IOException ex)
		{
			dirty = true; // just call it dirty
		}
		return dirty;
	}

}
