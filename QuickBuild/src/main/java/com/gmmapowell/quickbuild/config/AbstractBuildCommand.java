package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.core.AbstractStrategemTactic;

public abstract class AbstractBuildCommand extends AbstractStrategemTactic implements CanBeSkipped {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private List<BuildIfCommand> buildifs = new ArrayList<BuildIfCommand>();
	private boolean doubleQuick;

	public AbstractBuildCommand(TokenizedLine toks, ArgumentDefinition... args) {
		super(toks, args);
	}

	public AbstractBuildCommand(@SuppressWarnings("unchecked") Class<? extends ConfigApplyCommand>... clzs) {
		super(clzs);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	public void handleOptions(Config config) {
		for (ConfigApplyCommand opt : options)
		{
			opt.applyTo(config);
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
		else if (opt instanceof DoubleQuickCommand) {
			doubleQuick = true;
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

	@Override
	public boolean skipMe(BuildContext cxt) {
		return (doubleQuick && cxt.doubleQuick) || !isApplicable();
	}
}
