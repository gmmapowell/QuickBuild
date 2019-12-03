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

import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.utils.OrderedFileList;

public class GitHelper {
	public static String currentHead() {
		RunProcess rp = runGit(null, "rev-parse", "HEAD");
		return rp.getStdout().replaceAll("[^0-9a-fA-Z]", "");
	}

	public static RunProcess runGit(File inDir, String... args) {
		RunProcess proc = new RunProcess("git");
//		proc.debug(true);
		if (inDir != null)
			proc.executeInDir(inDir);
		proc.captureStdout();

		for (String a : args)
			proc.arg(a);
		proc.execute();
		return proc;
	}

	public static List<String> checkRepositoryClean() {
		RunProcess proc = runGit(null, "clean", "-ndf");
		
		List<String> ret = new ArrayList<String>();
		try {
			LineNumberReader gitReader = new LineNumberReader(new StringReader(proc.getStdout()));
			String line;
			while ((line = gitReader.readLine()) != null) {
				line = line.replace("Would remove ", "");
				if (new File(line).isDirectory())
					ret.add(line);
			}
			return ret;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static List<String> checkMissingCommits() {
		runGit(null, "fetch");
		RunProcess proc = runGit(null, "log", "master..origin/master", "--pretty=oneline");
		
		List<String> ret = new ArrayList<String>();
		try {
			LineNumberReader gitReader = new LineNumberReader(new StringReader(proc.getStdout()));
			String line;
			while ((line = gitReader.readLine()) != null)
				ret.add(line);
			return ret;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public static GitRecord checkFiles(boolean doComparison, OrderedFileList files, File file) {
		List<String> paths = new ArrayList<String>();
		if (files != null) {
			for (File f : files)
			{
				if (f.isDirectory())
					continue;
				String path = relPath(f).getPath();
				paths.add(path);
			}
		}

		GitRecord gittx = new GitRecord(file);
		boolean nofile = false;
		if (!gittx.sourceExists())
		{
			System.out.println("! No file: " + file);
			nofile = true;
			gittx.markDirty();
			gittx.fileMissing();
		}
		File newFile = null;
		try
		{
			doComparison &= file.exists();
			LineNumberReader oldReader = null;
			if (doComparison)
				oldReader = new LineNumberReader(new FileReader(file));
			newFile = new File(file.getParentFile(), file.getName() + ".new");
			gittx.generates(newFile);
			FileOutputStream fos = new FileOutputStream(newFile);
			PrintWriter pw = new PrintWriter(fos);
			int pos = 0;
			int fpos = 0;
			while (pos < paths.size()) {
				LineNumberReader gitReader;
				try {
					RunProcess proc = new RunProcess("git");
	//				proc.debug(true); 
					proc.executeInDir(FileUtils.getCurrentDir());
					proc.captureStdout();
					proc.arg("hash-object");
					while (pos < paths.size() && (pos == fpos || pos % 1000 != 0))
						proc.arg(paths.get(pos++));
					proc.execute();
					gitReader = new LineNumberReader(new StringReader(proc.getStdout()));
				} catch (Exception ex) {
					System.out.println(paths.size());
					throw ex;
				}
				boolean skipO = false;
				String currentOld = null;
				while (fpos < pos)
				{
					String f = paths.get(fpos++);
					String hash = gitReader.readLine();
					if (hash == null)
					{
						System.out.println("git did not return a hash for " + f);
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
								gittx.markDirty();
								continue;
							}
							else if (comp > 0)
							{
								System.out.println("> " + f);
								gittx.markDirty();
								skipO = true;
								break;
							}
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

	public static File relPath(File f) {
		if (FileUtils.isUnder(f, FileUtils.getCurrentDir()))
			return FileUtils.makeRelative(f);
		else
			return f;
	}
}
