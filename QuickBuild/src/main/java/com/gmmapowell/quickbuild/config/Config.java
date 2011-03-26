package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.Parent;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class Config implements Parent<ConfigCommand> {
	private String output;
	private List<ConfigCommand> commands = new ArrayList<ConfigCommand>();

	@Override
	public void addChild(ConfigCommand cmd) {
		if (cmd instanceof ConfigApplyCommand)
			((ConfigApplyCommand)cmd).applyTo(this);
		else
			commands.add(cmd);
	}

	public void setOutputDir(String output) {
		if (this.output != null)
			throw new QuickBuildException("You cannot set the output dir more than once");
		this.output = output;
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
