package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;


/* Other commands that may come in useful:
	# aapt.exe list bin/Fred.apk 
	# adb.exe devices
	# emulator.exe -avd my_avd
*/

public class AdbCommand implements BuildCommand {
	private final AndroidContext acxt;
	private final Project project;
	private List<String[]> commands = new ArrayList<String[]>();
	private List<BuildResource> requires = new ArrayList<BuildResource>();

	public AdbCommand(AndroidContext acxt, Project project) {
		this.acxt = acxt;
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	public void reinstall()
	{
		File apkFile = project.getOutput(project.getName()+".apk");
		command("install", "-r", apkFile.getPath());
		requires.add(new ApkResource(project, apkFile));
	}
	
	private void command(String... args) {
		commands.add(args);
	}

	@Override
	public List<BuildResource> generatedResources() {
		return null;
	}

	@Override
	public Set<String> getPackagesProvided() {
		return null;
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		if (commands.size() == 0)
			throw new QuickBuildException("No adb command specified");
		if (commands.size() != 1)
			throw new QuickBuildException("Undecided about this - allowing multiple commands seems reasonable, but how would it be specified?  Either you have an idea, or something is wrong");

		for (BuildResource br : requires)
			if (!cxt.requiresBuiltResource(this, br))
				return BuildStatus.RETRY;
		
		RunProcess proc = new RunProcess(acxt.getADB().getPath());
		proc.redirectStdout(System.out);
		proc.redirectStderr(System.out);
		
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
}
