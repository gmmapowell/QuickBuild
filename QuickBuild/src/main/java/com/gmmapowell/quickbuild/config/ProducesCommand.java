package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class ProducesCommand extends NoChildCommand implements ConfigApplyCommand {
	private String type;
	private String resource;
	private File resourceFile;
	
	public ProducesCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "type", "resource type"),
						   new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	@Override
	public void applyTo(Config config) {
		resourceFile = FileUtils.relativePath(resource);
	}

	public BuildResource getProducedResource(Strategem bash) {
		if (type.equals("jar"))
			return new JarResource(bash, resourceFile);
		else
			throw new UtilException("Cannot handle bash resource type " + type);
	}
	
	@Override
	public String toString() {
		return "Produces["+type+","+resource+"]";
	}

}
