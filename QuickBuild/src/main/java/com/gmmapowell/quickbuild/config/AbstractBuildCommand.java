package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;


public abstract class AbstractBuildCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private List<BuildIfCommand> buildifs = new ArrayList<BuildIfCommand>();

	public AbstractBuildCommand(Class<? extends ConfigApplyCommand>... clzs) {
		super(clzs);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	public void handleOptions(Config config) {
		for (ConfigApplyCommand opt : options)
		{
			if (handleOption(config, opt))
				continue;
			throw new UtilException("The option " + opt + " is not supported");
		}
	}

	public boolean handleOption(Config config, ConfigApplyCommand opt) {
		if (opt instanceof BuildIfCommand)
		{
			opt.applyTo(config);
			buildifs.add((BuildIfCommand) opt);
		}
		else
			return false;
		return true;
	}

	public boolean isApplicable() {
		for (BuildIfCommand b : buildifs)
			if (!b.isApplicable())
				return false;
		return true;
	}

}
