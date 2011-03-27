package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class RepoCommand extends NoChildCommand implements ConfigApplyCommand {
	private String repo;
	
	public RepoCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "repo", "maven package name"));
	}

	@Override
	public void applyTo(Config config) {
		if (repo.equals("-"))
			config.clearMavenRepos();
		else
			config.addMavenRepo(repo);
		
	}

}
