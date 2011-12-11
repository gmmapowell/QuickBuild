package com.gmmapowell.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class GitHelper {

	public static boolean checkFiles(boolean doComparison, OrderedFileList files, File file) {
		RunProcess proc = new RunProcess("git");
//		proc.debug(); 
		proc.executeInDir(FileUtils.getCurrentDir());
		proc.captureStdout();
		proc.arg("hash-object");
		List<String> paths = new ArrayList<String>();
		for (File f : files)
		{
			String path;
			if (FileUtils.isUnder(f, FileUtils.getCurrentDir()))
				path = FileUtils.makeRelative(f).getPath();
			else
				path = f.getPath();
			proc.arg(path);
			paths.add(path);
		}
		proc.execute();

		boolean dirty = false;
		boolean nofile = !file.exists();
		if (nofile)
		{
			System.out.println("! No file: " + file);
			dirty = true;
		}
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
			boolean skipO = false;
			String lastO = null;
			for (String f : paths)
			{
				String s = r.readLine();
				if (s == null)
				{
					System.out.println("! Inconsistent number of files and hashes");
					dirty = true;
					break;
				}
				String nextLine = s + " " + f;
				pw.println(nextLine);
				while (true)
				{
					if (nofile)
						break;
					if (skipO)
						skipO = false;
					else if (old != null)
						lastO = old.readLine();
					if (lastO != null && lastO.equals(nextLine))
						break;
					else
					{
						dirty = true;
						if (lastO == null)
						{
							System.out.println("> " + f);
							break;
						}
						String oldFile = lastO.substring(41);
						int comp = oldFile.compareToIgnoreCase(f);
						if (comp == 0)
						{
							System.out.println("| " + f);
							break;
						}
						else if (comp < 0)
						{
							System.out.println("< " + oldFile);
							continue;
						}
						else if (comp > 0)
						{
							System.out.println("> " + f);
							skipO = true;
							break;
						}
					}
				}
			}
			{
				String o;
				while (old != null && (o = old.readLine()) != null)
					System.out.println("< " + o);
			}
			if (old != null)
				old.close();
			pw.close();
			fos.close();
			if (doComparison)
			{
				if (dirty)
				{
					boolean fd = file.delete();
					if (!fd)
						throw new UtilException("Could not delete the file " + file + " when renaming " + newFile);
					boolean renameWorked = newFile.renameTo(file);
					if (!renameWorked)
						throw new UtilException("Could not rename " + newFile + " to " + file);
				}
				else
				{
					// by defn, they're the same, so remove the new one
					newFile.delete();
				}
			}
		}
		catch (IOException ex)
		{
			System.out.println("Exception encountered in git checking: " + ex.getMessage());
			System.out.println("Returning dirty status");
			dirty = true; // just call it dirty
			file.delete();
			if (newFile != null)
				newFile.delete();
		}
		return dirty;
	}

}
