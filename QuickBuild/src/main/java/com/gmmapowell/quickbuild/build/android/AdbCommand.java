package com.gmmapowell.quickbuild.build.android;

import java.io.File;
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
import com.gmmapowell.utils.StringUtil;


/* Other commands that may come in useful:
	# aapt.exe list bin/Fred.apk 
	# adb.exe devices
	# emulator.exe -avd my_avd
*/

public class AdbCommand implements Tactic {
	public class Command {

		final Object[] args;
		private BuildStatus stat =  BuildStatus.BROKEN;

		public Command(Object[] args) {
			this.args = args;
		}

		public void errorStatus(BuildStatus stat) {
			this.stat  = stat;
		}

	}

	private final AndroidContext acxt;
	private List<Command> commands = new ArrayList<Command>();
	private final BuildResource apk;
	private final Strategem parent;

	public AdbCommand(AndroidContext acxt, Strategem parent, StructureHelper files, BuildResource apk) {
		this.acxt = acxt;
		this.parent = parent;
		this.apk = apk;
	}

	public void reinstall()
	{
		command("install", "-r", apk.getPath()).errorStatus(BuildStatus.TEST_FAILURES);
	}
	
	private Command command(Object... args) {
		Command command = new Command(args);
		commands.add(command);
		return command;
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
		
		Command cmd = commands.get(0);
		for (Object s : cmd.args)
		{
			if (s instanceof String)
				proc.arg((String) s);
			else if (s instanceof File)
				proc.arg(((File)s).getPath());
			else if (s instanceof PendingResource)
				proc.arg(((PendingResource) s).getPath().getPath());
			else
				throw new UtilException("Cannot handle argument of type " + s.getClass());
		}
		
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			// It doesn't always return an error code .. eg. "INSTALL_FAILED_DEXOPT"
			for (String s : StringUtil.lines(proc.getStderr()))
			{
				if (s.trim().isEmpty())
					continue;
				if (s.contains(" KB/s "))
					continue;
				System.out.println(s);
			}
			return BuildStatus.SUCCESS;
		}
		else if (proc.getStderr().contains("error: device not found"))
		{
			System.out.println("Device not found - ignoring");
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return cmd.stat;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Command cmd : commands)
		{
			if (sb.length() > 0)
				sb.append("\n");
			sb.append("adb");
			for (Object s : cmd.args)
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
