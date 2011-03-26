package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigCommand {
	private List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String target;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "target", "jar target"));
		target = target.toLowerCase();
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  jar " + target + "\n");
		return sb.toString();
	}
}
