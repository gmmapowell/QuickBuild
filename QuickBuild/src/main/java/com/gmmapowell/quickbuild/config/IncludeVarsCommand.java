package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import org.zinutils.utils.FileUtils;

public class IncludeVarsCommand extends NoChildCommand implements ConfigApplyCommand {
	protected String file;
	
	public IncludeVarsCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "file", "file to read from")
		);
	}

	@Override
	public void applyTo(Config config) {
		for (String s : FileUtils.readFileAsLines(new File(file))) {
			s = s.trim();
			if (s.length() == 0 || s.startsWith("//") || s.startsWith("#"))
				continue;
			int idx = s.indexOf('=');
			if (idx == -1) {
				System.out.println("Cannot understand var defn: " + s + " in file " + file);
				continue;
			}
			String name = s.substring(0,idx).trim();
			String val = s.substring(idx+1).trim();
			config.setVarProperty(name, val);
		}
	}

}
