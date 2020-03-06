package com.gmmapowell.quickbuild.build.java;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class DefineComboCommand extends NoChildCommand implements ConfigApplyCommand {
	private String combo;
	private List<String> resources = new ArrayList<>();
	private JavaNature jn;
	
	public DefineComboCommand(TokenizedLine toks)
	{
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "combo", "target"),
			new ArgumentDefinition("*", Cardinality.ONE_OR_MORE, "resources", "resources"));
	}

	@Override
	public void applyTo(Config config) {
		jn = config.getNature(JavaNature.class);
		jn.addDefiningCombo(this);
	}

	public void addToCombos() {
		for (String s : resources) {
			if (jn.hasCombo(s) ) {
				jn.addComboToBiggerCombo(combo, s);
			} else
				jn.addComboJar(combo, new PendingResource(s));
		}
	}
}
