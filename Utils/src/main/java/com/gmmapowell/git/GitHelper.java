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

import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class GitHelper {

	public static RunProcess runGit(File inDir, String... args) {
		RunProcess proc = new RunProcess("git");
//		proc.debug(true);
		proc.executeInDir(inDir);
		proc.captureStdout();

		for (String a : args)
			proc.arg(a);
		proc.execute();
		return proc;
	}

	public static GitRecord checkFiles(boolean doComparison, OrderedFileList files, File file) {
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

		GitRecord gittx = new GitRecord(file);
		boolean nofile = false;
		if (!gittx.sourceExists())
		{
			System.out.println("! No file: " + file);
			nofile = true;
			gittx.setDirty();
			gittx.fileMissing();
		}
		File newFile = null;
		try
		{
			doComparison &= file.exists();
			LineNumberReader gitReader = new LineNumberReader(new StringReader(proc.getStdout()));
			LineNumberReader oldReader = null;
			newFile = new File(file.getParentFile(), file.getName() + ".new");
			gittx.generates(newFile);
			FileOutputStream fos = new FileOutputStream(newFile);
			if (doComparison)
				oldReader = new LineNumberReader(new FileReader(file));
			
			PrintWriter pw = new PrintWriter(fos);
			boolean skipO = false;
			String currentOld = null;
			for (String f : paths)
			{
				String hash = gitReader.readLine();
				if (hash == null)
				{
					System.out.println(">! " + f);
					gittx.dirtyFile(new File(f));
					continue;
				}
				String nextLine = hash + " " + f;
				pw.println(nextLine);
				while (true)
				{
					if (nofile)
						break;
					if (skipO)
						skipO = false;
					else if (oldReader != null)
						currentOld = oldReader.readLine();
					if (currentOld != null && currentOld.equals(nextLine)) {
						break;
					} 
					else
					{
						if (currentOld == null)
						{
							if (oldReader != null)
								System.out.println("> " + f);
							gittx.dirtyFile(new File(f));
							break;
						}
						String oldFile = currentOld.substring(41);
						int comp = oldFile.compareToIgnoreCase(f);
						if (comp == 0)
						{
							System.out.println("| " + f);
							gittx.dirtyFile(new File(f));
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
				while (oldReader != null && (o = oldReader.readLine()) != null)
					System.out.println("< " + o);
			}
			if (oldReader != null)
				oldReader.close();
			pw.close();
			fos.close();
		}
		catch (IOException ex)
		{
			System.out.println("Exception encountered in git checking: " + ex.getMessage());
			System.out.println("Returning dirty status");
			gittx.setError();
		}
		return gittx;
	}

}
