package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.StringUtil;


/* Other commands that may come in useful:
	# aapt.exe list bin/Fred.apk 
	# adb.exe devices
	# emulator.exe -avd my_avd
*/

public class AdbCommand extends AbstractTactic {
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
	private final BuildResource builds;
	private String device;

	public AdbCommand(AndroidContext acxt, Strategem parent, StructureHelper files, BuildResource apk, BuildResource builds) {
		super(parent);
		this.acxt = acxt;
		this.apk = apk;
		this.builds = builds;
	}
	
	public void setDevice(String device) {
		this.device = device;
	}

	public void reinstall()
	{
		command("install", "-r", apk.getPath()).errorStatus(BuildStatus.TEST_FAILURES);
	}
	
	public void start(String activity, List<String> extras) {
		Object[] args = new Object[5+extras.size()*3];
		args[0] = "shell";
		args[1] = "am";
		args[2] = "start";
		args[3] = "-n";
		int pos = 4;
		for (int i=0;i<extras.size();i++) {
			args[pos++] = "-e";
			String[] foo = extras.get(i).split("=");
			args[pos++] = foo[0];
			args[pos++] = foo[1];
		}
		args[pos] = activity;
		command(args);
	}
	
	public void instrument(String activity, List<String> extras) {
		Object[] args = new Object[5+extras.size()*3];
		args[0] = "shell";
		args[1] = "am";
		args[2] = "instrument";
		args[3] = "-w";
		int pos = 4;
		for (int i=0;i<extras.size();i++) {
			args[pos++] = "-e";
			String[] foo = extras.get(i).split("=");
			args[pos++] = foo[0];
			args[pos++] = foo[1];
		}
		args[pos] = activity;
		command(args);
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

		List<String> devices = acxt.getConnectedDeviceList();
		BuildStatus ret = BuildStatus.SUCCESS;
		if (devices.isEmpty())
			throw new UtilException("There are no connected devices");
		else if (device != null) {
			if (!devices.contains(device))
				throw new UtilException("Device " + device + " is not connected");
			ret = runOnDevice(cxt, device);
		} else {
			for (String d : devices) {
				System.out.println("  running adb on " + d);
				ret = runOnDevice(cxt, d);
				if (ret != BuildStatus.SUCCESS && ret != BuildStatus.BACKGROUND)
					break;
			}
		}
		
		return ret;
	}
	
	public BuildStatus runOnDevice(BuildContext cxt, String d) {
		RunProcess proc = new RunProcess(acxt.getADB().getPath());
		proc.captureStdout();
		proc.captureStderr();
		
		Command cmd = commands.get(0);
		proc.arg("-s");
		proc.arg(d);
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
				if (s.contains("Fail"))
					return BuildStatus.BROKEN;
			}
			for (String s : StringUtil.lines(proc.getStdout()))
			{
				if (s.trim().isEmpty())
					continue;
				if (s.contains(" KB/s "))
					continue;
				if (s.contains("pkg: "))
					continue;
				if (s.contains("Success"))
					continue;
				System.out.println(s);
				if (s.contains("Fail"))
					return BuildStatus.BROKEN;
			}
			cxt.builtResource(builds);
			return BuildStatus.SUCCESS;
		}
		else if (proc.getStderr().contains("error: device not found"))
		{
			System.out.println("Device not found - ignoring");
			cxt.builtResource(builds);
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
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "adb");
	}
}
