package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.IncludePackageCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import org.zinutils.utils.FileUtils;

public class ResourceCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigApplyCommand {
	private String resource;
	private PendingResource pending;
	private List<File> includePackages;
	private List<File> excludePackages;
	
	@SuppressWarnings("unchecked")
	public ResourceCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	@Override
	public void applyTo(Config config) {
		pending = new PendingResource(resource);
	}

	public PendingResource getPendingResource() {
		return pending;
	}

	private void includePackage(IncludePackageCommand ipc) {
		if (ipc.isExclude())
		{
			if (includePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (excludePackages == null)
				excludePackages = new ArrayList<File>();
			excludePackages.add(FileUtils.convertDottedToPath(ipc.getPackage()));
		}
		else
		{
			if (excludePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (includePackages == null)
				includePackages = new ArrayList<File>();
			includePackages.add(FileUtils.convertDottedToPath(ipc.getPackage()));
		}
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		if (obj instanceof IncludePackageCommand)
		{
			includePackage((IncludePackageCommand) obj);
		}
		else
			throw new UtilException("ResourceCommand cannot handle child " + obj);
	}

	@Override
	public String toString() {
		return "Resource["+resource+"]";
	}

	public boolean includes(String name) {
		if (includePackages == null && excludePackages == null)
			return true;
		else if (includePackages != null)
		{
			for (File s : includePackages)
				if (name.startsWith(FileUtils.posixPath(s)))
					return true;
			return false;
		}
		else // check excludePackages
		{
			for (File s : excludePackages)
				if (name.startsWith(FileUtils.posixPath(s)))
					return false;
			return true;
		}
	}
}
