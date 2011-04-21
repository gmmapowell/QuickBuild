package com.gmmapowell.quickbuild.build.android;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;


/* Other commands that may come in useful:
	# aapt.exe list bin/Fred.apk 
	# adb.exe devices
	# emulator.exe -avd my_avd
*/

public class AdbCommand implements Tactic {
	private final AndroidContext acxt;
	private final Strategem parent;
	private List<String[]> commands = new ArrayList<String[]>();
	private List<BuildResource> requires = new ArrayList<BuildResource>();
	private final StructureHelper files;
	private final SolidResource apk;

	public AdbCommand(AndroidContext acxt, Strategem parent, StructureHelper files, SolidResource apk) {
		this.acxt = acxt;
		this.parent = parent;
		this.files = files;
		this.apk = apk;
	}

	public void reinstall()
	{
		command("install", "-r", apk.getPath().getPath());
		requires.add(apk);
	}
	
	private void command(String... args) {
		commands.add(args);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		if (commands.size() == 0)
			throw new QuickBuildException("No adb command specified");
		if (commands.size() != 1)
			throw new QuickBuildException("Undecided about this - allowing multiple commands seems reasonable, but how would it be specified?  Either you have an idea, or something is wrong");

		/* TODO: not my problem
		for (BuildResource br : requires)
			if (!cxt.requiresBuiltResource(this, br))
				return BuildStatus.RETRY;
		*/
		
		RunProcess proc = new RunProcess(acxt.getADB().getPath());
		proc.captureStdout();
		proc.captureStderr();
		
		for (String s : commands.get(0))
			proc.arg(s);
		
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String[] cmd : commands)
		{
			if (sb.length() > 0)
				sb.append("\n");
			sb.append("adb");
			for (String s : cmd)
				sb.append(" " + s);
		}
		return sb.toString();
	}

	@Override
	public Strategem belongsTo() {
		// TODO Auto-generated method stub
		return null;
	}
}
