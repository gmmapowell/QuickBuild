package com.gmmapowell.quickbuild.config;

import java.io.File;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

import com.gmmapowell.quickbuild.build.java.DirectoryResource;
import com.gmmapowell.quickbuild.build.java.JarDirectoryResource;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.build.javascript.JSFileResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class ProducesCommand extends NoChildCommand implements ConfigApplyCommand {
	private String type;
	private String resource;
	private File resourceFile;
	private boolean analyze;
	
	public ProducesCommand(TokenizedLine toks)
	{
		toks.process(this, 
			new ArgumentDefinition("--analyze", Cardinality.OPTION, "analyze", "do analysis on produced resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "type", "resource type"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	@Override
	public void applyTo(Config config) {
		resourceFile = new File(resource);
	}

	public boolean doAnalysis()
	{
		return analyze;
	}
	
	public BuildResource getProducedResource(Tactic t) {
		if (type.equals("jar"))
			return new JarResource(t, resourceFile);
		else if (type.equals("js"))
			return new JSFileResource(t, resourceFile);
		else if (type.equals("classdir"))
			return new DirectoryResource(t, resourceFile);
		else if (type.equals("libdir"))
			return new JarDirectoryResource(t, resourceFile);
		else
			throw new UtilException("Cannot handle bash resource type " + type);
	}
	
	@Override
	public String toString() {
		return "Produces["+type+","+resource+"]";
	}

}
