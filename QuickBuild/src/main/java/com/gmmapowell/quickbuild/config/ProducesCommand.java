package com.gmmapowell.quickbuild.config;

import java.io.File;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.DirectoryResource;
import com.gmmapowell.quickbuild.build.java.JarDirectoryResource;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.build.java.WarResource;
import com.gmmapowell.quickbuild.build.javascript.JSFileResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ProducesCommand extends NoChildCommand implements ConfigApplyCommand {
	private String type;
	private String resource;
	private File resourceFile;
	private boolean analyze;
	private File execdir;
	
	public ProducesCommand(TokenizedLine toks)
	{
		toks.process(this, 
			new ArgumentDefinition("--analyze", Cardinality.OPTION, "analyze", "do analysis on produced resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "type", "resource type"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	public void execdir(File execdir) {
		this.execdir = execdir;
	}

	@Override
	public void applyTo(Config config) {
		StructureHelper files = new StructureHelper(execdir, "");
		resourceFile = files.getRelative(resource);
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
		else if (type.equals("zip"))
			return new WarResource(t, resourceFile);
		else
			throw new UtilException("Cannot handle bash resource type " + type);
	}
	
	@Override
	public String toString() {
		return "Produces["+type+","+resource+"]";
	}
}
