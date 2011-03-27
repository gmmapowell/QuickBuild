package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class Config extends SpecificChildrenParent<ConfigCommand>  {
	private String output;
	private List<ConfigBuildCommand> commands = new ArrayList<ConfigBuildCommand>();
	private List<BuildCommand> buildcmds = new ArrayList<BuildCommand>();

	@SuppressWarnings("unchecked")
	public Config()
	{
		super(ConfigApplyCommand.class, ConfigBuildCommand.class);
	}
	
	@Override
	public void addChild(ConfigCommand cmd) {
		if (cmd instanceof ConfigApplyCommand)
			((ConfigApplyCommand)cmd).applyTo(this);
		else if (cmd instanceof ConfigBuildCommand)
			commands.add((ConfigBuildCommand) cmd);
		else
			throw new QuickBuildException("'" + cmd + "' is not an acceptable child");
	}
	
	public void setOutputDir(String output) {
		if (this.output != null)
			throw new QuickBuildException("You cannot set the output dir more than once");
		this.output = output;
	}

	public File getOutputDir(File forProject) {
		return new File(forProject, output);
	}
	
	public void done() {
		for (ConfigBuildCommand c : commands)
		{
			buildcmds.addAll(c.buildCommands(this));
		}
	}
	
	public List<BuildCommand> getBuildCommandsInOrder() {
		return buildcmds;
	}

	/** Copy across all the packages which are defined in global things to a build context
	 * @param availablePackages the map to copy into
	 */
	public void supplyPackages(Map<String, String> availablePackages) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("output = " + output + "\n");
		sb.append("Commands:\n");
		for (ConfigCommand cc : commands)
			sb.append(cc);
		return sb.toString();
	}
}
