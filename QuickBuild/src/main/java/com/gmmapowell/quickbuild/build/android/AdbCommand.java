package com.gmmapowell.quickbuild.build.android;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;


/* Other commands that may come in useful:
	# aapt.exe list bin/Fred.apk 
	# adb.exe devices
	# emulator.exe -avd my_avd
*/

public class AdbCommand implements Tactic {
	private final AndroidContext acxt;
	private List<Object[]> commands = new ArrayList<Object[]>();
	private final BuildResource apk;
	private final Strategem parent;

	public AdbCommand(AndroidContext acxt, Strategem parent, StructureHelper files, BuildResource apk) {
		this.acxt = acxt;
		this.parent = parent;
		this.apk = apk;
	}

	public void reinstall()
	{
		command("install", "-r", apk);
	}
	
	private void command(Object... args) {
		commands.add(args);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (commands.size() == 0)
			throw new QuickBuildException("No adb command specified");
		if (commands.size() != 1)
			throw new QuickBuildException("Undecided about this - allowing multiple commands seems reasonable, but how would it be specified?  Either you have an idea, or something is wrong");

		RunProcess proc = new RunProcess(acxt.getADB().getPath());
		proc.captureStdout();
		proc.captureStderr();
		
		for (Object s : commands.get(0))
		{
			if (s instanceof String)
				proc.arg((String) s);
			else if (s instanceof PendingResource)
				proc.arg(((PendingResource) s).getPath().getPath());
			else
				throw new UtilException("Cannot handle argument of type " + s.getClass());
		}
		
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
		for (Object[] cmd : commands)
		{
			if (sb.length() > 0)
				sb.append("\n");
			sb.append("adb");
			for (Object s : cmd)
				sb.append(" " + s);
		}
		return sb.toString();
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "adb");
	}
}
