package com.gmmapowell.quickbuild.build.maven;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class MavenLibraryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String pkg;
	private final List<String> context = new ArrayList<String>();
	private final List<String> combos = new ArrayList<String>();
	
	public MavenLibraryCommand(TokenizedLine toks) {
		toks.process(this, 
			new ArgumentDefinition("--context", Cardinality.ZERO_OR_MORE, "context", "context"),
			new ArgumentDefinition("--combo", Cardinality.ZERO_OR_MORE, "combos", "combos"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "pkg", "maven package name"));
	}

	@Override
	public void applyTo(Config config) {
		MavenNature n = config.getNature(MavenNature.class);
		MavenResource res = n.loadPackage(pkg);
		for (String c : context)
			config.bindLibraryContext(c, pkg);
		JavaNature jn = config.getNature(JavaNature.class);
		for (String s : combos)
			jn.addComboJar(s, res);
	}

}
