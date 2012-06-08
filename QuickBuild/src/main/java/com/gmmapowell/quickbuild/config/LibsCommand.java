package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.ExcludeCommand;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class LibsCommand extends SpecificChildrenParent<ConfigApplyCommand>  implements ConfigApplyCommand {
	private final File libsDir;
	private String libs;
	private List<ExcludeCommand> exclusions = new ArrayList<ExcludeCommand>();

	@SuppressWarnings("unchecked")
	public LibsCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "libs", "target"));
		if (libs.equals("/"))
			libsDir = null;
		else
			libsDir = new File(libs);
	}
	
	@Override
	public void addChild(ConfigApplyCommand obj) {
		if (obj instanceof ExcludeCommand)
		{
			exclusions.add((ExcludeCommand) obj);
		}
		else
			throw new UtilException("The option " + obj + " is not valid for LibsCommand");
	}

	@Override
	public void applyTo(Config config) {
		JavaNature n = config.getNature(JavaNature.class);
		if (libsDir == null)
			n.cleanLibDirs();
		else
			n.addLib(libsDir, exclusions);
	}

}
