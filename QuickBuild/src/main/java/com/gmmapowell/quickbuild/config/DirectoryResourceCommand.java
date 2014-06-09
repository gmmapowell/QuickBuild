package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.IncludePackageCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;

public class DirectoryResourceCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigApplyCommand {
	private String dir;
	private String prefix;
	private List<String> includePackages;
	private List<String> excludePackages;
	public File rootDir;
	
	@SuppressWarnings("unchecked")
	public DirectoryResourceCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "dir"), new ArgumentDefinition("*", Cardinality.OPTION, "prefix", "prefix"));
		rootDir = FileUtils.relativePath(dir);
	}

	@Override
	public void applyTo(Config config) {
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		if (obj instanceof IncludePackageCommand)
		{
			IncludePackageCommand ipc = (IncludePackageCommand) obj;
			if (ipc.isExclude())
			{
				if (includePackages != null)
					throw new UtilException("Cannot request both include and exclude packages for " + this);
				if (excludePackages == null)
					excludePackages = new ArrayList<String>();
				excludePackages.add(ensureSlash(ipc.getPackage()));
			}
			else
			{
				if (excludePackages != null)
					throw new UtilException("Cannot request both include and exclude packages for " + this);
				if (includePackages == null)
					includePackages = new ArrayList<String>();
				includePackages.add(ensureSlash(ipc.getPackage()));
			}
		}
		else
			throw new UtilException("ResourceCommand cannot handle child " + obj);
	}

	private String ensureSlash(String name) {
		if (!name.endsWith("/"))
			return name +"/";
		return name;
	}

	@Override
	public String toString() {
		return "DirectoryResource["+dir+"]";
	}

	public boolean includes(String name) {
		if (includePackages == null && excludePackages == null)
			return true;
		else if (includePackages != null)
		{
			for (String s : includePackages)
				if (name.startsWith(s))
					return true;
			return false;
		}
		else // check excludePackages
		{
			for (String s : excludePackages)
				if (name.startsWith(s))
					return false;
			return true;
		}
	}

	public String prefix(String name) {
		if (prefix == null)
			return name;
		return prefix +"/"+ name;
	}
}
