package com.gmmapowell.quickbuild.build.java;

import java.util.Set;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class UseComboCommand extends NoChildCommand implements ConfigApplyCommand {
	private String combo;
	private JavaNature jn;
	
	public UseComboCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "combo", "target"));
	}

	@Override
	public void applyTo(Config config) {
		jn = config.getNature(JavaNature.class);
	}

	public PendingResource getResource() {
		return new PendingResource(combo);
	}

	public Set<BuildResource> resources() {
		return jn.comboResources(combo);
	}

	public void needComboLib(JavaCommand javaCommand) {
		jn.needsCombo(javaCommand, combo);
	}

	public void needComboLib(JarCommand jarCommand) {
		jn.needsCombo(jarCommand, combo);
	}

}
