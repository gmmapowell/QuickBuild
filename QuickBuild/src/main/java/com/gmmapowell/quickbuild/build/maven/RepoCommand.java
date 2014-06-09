package com.gmmapowell.quickbuild.build.maven;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class RepoCommand extends NoChildCommand implements ConfigApplyCommand {
	private String repo;
	
	public RepoCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "repo", "maven package name"));
	}

	@Override
	public void applyTo(Config config) {
		MavenNature n = config.getNature(MavenNature.class);
		if (repo.equals("-"))
			n.clearMavenRepos();
		else
			n.addMavenRepo(repo);
		
	}

}
