package com.gmmapowell.quickbuild.build.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class GitIdCommand extends NoChildCommand implements ConfigApplyCommand {
	private String label;
	
	public GitIdCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "label", "label"));
	}

	@Override
	public void applyTo(Config config) {
	}

	public void writeTrackerFile(BuildContext cxt, JarOutputStream jos, String dir, String identifier) throws IOException {
		jos.putNextEntry(new JarEntry(dir + "/" + label + ".git"));
		PrintWriter pw = new PrintWriter(jos);
		String head = System.getenv("BUILD_VCS_NUMBER");
		if (head == null || head.trim().length() == 0)
			head = GitHelper.currentHead();
		System.out.println("Setting " + label + ".git to " + head);
		FileUtils.createFile(cxt.getGitCacheFile(identifier, "-gitid"), head);
		pw.print(head);
		pw.flush();
	}

	public void writeTrackerFile(BuildContext cxt, ZipArchiveOutputStream jos, String dir, String identifier) throws IOException {
		jos.putArchiveEntry(new ZipArchiveEntry(dir + "/" + label + ".git"));
		PrintWriter pw = new PrintWriter(jos);
		String head = System.getenv("BUILD_VCS_NUMBER");
		if (head == null || head.trim().length() == 0)
			head = GitHelper.currentHead();
		System.out.println("Setting " + label + ".git to " + head);
		FileUtils.createFile(cxt.getGitCacheFile(identifier, "-gitid"), head);
		pw.print(head);
		pw.flush();
	}
}
