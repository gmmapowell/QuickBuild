package com.gmmapowell.quickbuild.build.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.zinutils.git.GitHelper;
import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class GitIdCommand extends NoChildCommand implements ConfigApplyCommand {
	private String label;
	
	public GitIdCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "label", "label"));
	}

	@Override
	public void applyTo(Config config) {
	}

	public void writeTrackerFile(JarOutputStream jos, String dir) throws IOException {
		jos.putNextEntry(new JarEntry(dir + "/" + label + ".git"));
		PrintWriter pw = new PrintWriter(jos);
		String head = System.getenv("BUILD_VCS_NUMBER");
		if (head == null || head.trim().length() == 0)
			head = GitHelper.currentHead();
		System.out.println("Setting " + label + ".git to " + head);
		pw.print(head);
		pw.flush();
	}
}
