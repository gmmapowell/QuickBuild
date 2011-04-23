package com.gmmapowell.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;

import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class GitHelper {

	public static boolean checkFiles(boolean doComparison, OrderedFileList files, File file) {
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
		File newFile = null;
		try
		{
			doComparison &= file.exists();
			LineNumberReader r = new LineNumberReader(new StringReader(proc.getStdout()));
			LineNumberReader old = null;
			FileOutputStream fos;
			if (doComparison)
			{
				newFile = new File(file.getParentFile(), file.getName() + ".new");
				fos = new FileOutputStream(newFile);
				old = new LineNumberReader(new FileReader(file));
			}
			else
				fos = new FileOutputStream(file);
			
			PrintWriter pw = new PrintWriter(fos);
			for (File f : files)
			{
				String s = r.readLine();
				if (s == null)
				{
					dirty = true;
					break;
				}
				String nextLine = s + " " + FileUtils.makeRelative(f).getPath();
				pw.println(nextLine);
				if (old != null)
				{
					String o = old.readLine();
					if (!o.equals(nextLine))
					{
						System.out.println("Files differ at file " + old.getLineNumber() +":");
						System.out.println("  " + o);
						System.out.println("  " + nextLine);
						old.close();
						old = null;
						dirty = true;
					}
				}
			}
			pw.close();
			fos.close();
			if (doComparison)
			{
				if (dirty)
				{
					file.delete();
					newFile.renameTo(file);
				}
				else
					newFile.delete(); // by defn, they're the same, so remove the new one
			}
		}
		catch (IOException ex)
		{
			dirty = true; // just call it dirty
			file.delete();
			newFile.delete();
		}
		return dirty;
	}

}
