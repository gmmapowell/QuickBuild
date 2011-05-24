package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.PrettyPrinter;

public class ErrorCase {

	private BuildStatus outcome;
	private final ItemToBuild itb;
	private final List<String> args;
	private final String stdout;
	private final String stderr;
	private final List<String> summaryLines = new ArrayList<String>();
	private final File logDir;

	public ErrorCase(File logDir, ItemToBuild itb, List<String> args, String stdout, String stderr) {
		this.logDir = logDir;
		this.itb = itb;
		this.args = args;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	public static final Comparator<? super ErrorCase> Comparator = new Comparator<ErrorCase>() {
		@Override
		public int compare(ErrorCase o1, ErrorCase o2) {
			int ret = o1.outcome.compareTo(o2.outcome);
			if (ret != 0)
				return ret;
			ret = o1.itb.compareTo(o2.itb);
			if (ret != 0)
				return ret;
			return 0;
		}
	};
	
	public void addMessage(String s) {
		summaryLines.add(s);
	}

	public void flush(BuildStatus outcome) {
		try
		{
			this.outcome = outcome;
			PrettyPrinter pp = new PrettyPrinter();
			summary(pp);
			pp.requireNewline();
			pp.append("-----");
			pp.requireNewline();
			pp.append(args);
			pp.requireNewline();
			pp.append("-----");
			pp.requireNewline();
			pp.append(stdout);
			pp.requireNewline();
			pp.append("-----");
			pp.requireNewline();
			pp.append(stderr);
			pp.requireNewline();
			
			FileUtils.assertDirectory(logDir);
			PrintWriter pw = new PrintWriter(new FileOutputStream(getLogFile(), true));
			pw.print(pp);
			pw.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public void summary(PrettyPrinter pp) {
		pp.append(outcome + ": " + itb.id + " " + itb.label);
		pp.indentMore();
		for (String s : summaryLines)
		{
			pp.append(s);
			pp.requireNewline();
		}
		pp.indentLess();
	}

	private File getLogFile() {
		return new File(logDir, FileUtils.clean(((ExecuteStrategem)itb.strat).name()));
	}

}
