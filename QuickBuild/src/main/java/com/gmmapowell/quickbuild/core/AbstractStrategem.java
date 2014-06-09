package com.gmmapowell.quickbuild.core;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import org.zinutils.utils.ArgumentDefinition;

public abstract class AbstractStrategem extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	protected final List<Tactic> tactics = new ArrayList<Tactic>();

	@SuppressWarnings("unchecked")
	public AbstractStrategem(TokenizedLine toks, ArgumentDefinition... args) {
		toks.process(this, args);
	}

	public AbstractStrategem(Class<? extends ConfigApplyCommand>[] clzs) {
		super(clzs);
	}

	@Override
	public final List<? extends Tactic> tactics() {
		return tactics;
	}
}
