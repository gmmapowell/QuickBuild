package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;
import java.io.FileReader;
import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategemTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public class DevenvCommand extends AbstractStrategemTactic {
	private String projectName;
	private File rootdir;
	private OrderedFileList sources;
	private ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private StructureHelper files;

	public DevenvCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		
	}

	@Override
	public Strategem applyConfig(Config config) {
		try
		{
			files = new StructureHelper(rootdir, config.getOutput());
			sources = new OrderedFileList(rootdir, "*.cs");
			sources.add(rootdir, "*.xaml");
			sources.add(rootdir, "*.csproj");
			sources.add(rootdir, "*.sln");
			for (File f : sources)
			{
				if (f.getName().endsWith(".csproj"))
				{
					// figure out what the resource is
					LinePatternParser lpp = new LinePatternParser();
					lpp.match("<OutputType>(.*)</OutputType>", "output", "type");
					lpp.match("<OutputPath>(.*)</OutputPath>", "build", "path");
					lpp.match("<AssemblyName>(.*)</AssemblyName>", "assembly", "name");
					lpp.match("<XapFilename>(.*)</XapFilename>", "xap", "filename");
					FileReader fr = new FileReader(f);
					String type = null;
					String path = null;
					String assembly = null;
					String xap = null;
					for (LinePatternMatch lpm : lpp.applyTo(fr))
					{
						if (lpm.is("output"))
							type = lpm.get("type");
						else if (lpm.is("assembly"))
							assembly = lpm.get("name");
						else if (lpm.is("build"))
						{
							// Doing this right seems very hard :-(
							if (!lpm.get("path").contains("Release"))
								path = lpm.get("path").replace('\\', '/');
						}
						else if (lpm.is("xap"))
							xap = lpm.get("filename");
						else {
							fr.close();
							throw new QuickBuildException("Cannot handle " + lpm);
						}
					}
					if (type.equals("Library"))
					{
						if (xap != null)
							builds.add(new XAPResource(this, new File(f.getParentFile(), path+"/"+xap)));
						else
							builds.add(new DLLResource(this, new File(f.getParentFile(), path+"/"+assembly+".dll")));
					}
					else if (type.equals("Exe"))
					{
						builds.add(new EXEResource(this, new File(f.getParentFile(), path +"/" + assembly+".exe")));
					}
					else
						throw new QuickBuildException("Cannot handle msbuild type " + type);
					fr.close();
				}
			}
			}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
		return this;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		CsNature nature = cxt.getNature(CsNature.class);
		if (nature == null || !nature.isAvailable())
		{
			System.out.println("CsNature not available ... attempting to copy");
			BuildStatus ret = BuildStatus.SKIPPED;
			File msequiv = null;
			if (cxt.hasPath("msequiv"))
				msequiv = cxt.getPath("msequiv");
			for (BuildResource br : builds)
			{
				File f = br.getPath();
				if (msequiv != null)
				{
					File actual = new File(msequiv, FileUtils.makeRelative(br.getPath()).getPath());
					File copy = FileUtils.relativePath(f);
					if (actual.exists() && (!copy.exists() || !FileUtils.isUpToDate(copy, actual)))
					{
						FileUtils.copyAssertingDirs(actual, copy);
						ret = BuildStatus.SUCCESS;
					}
				}
				cxt.builtResource(br);
			}
			return ret;
		}
		RunProcess proc = new RunProcess(nature.getDevenv());
		proc.debug(showDebug);
		proc.captureStderr();
		proc.captureStdout();
		proc.arg(files.getRelative(projectName+".sln").getPath());
		proc.arg("/rebuild");
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			for (BuildResource br : builds)
				cxt.builtResource(br);
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStdout());
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "Devenv["+projectName+"]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return new ResourcePacket<PendingResource>();
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return sources;
	}

	@Override
	public boolean onCascade() {
		return false;
	}
	
	@Override
	public String toString() {
		return identifier();
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}
}
