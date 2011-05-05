package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class WarBuildCommand implements Tactic {

	private final WarCommand parent;
	private final File warfile;
	private final StructureHelper files;
	private final List<File> dirsToJar = new ArrayList<File>();
	private final List<PendingResource> warlibs;
	private final List<Pattern> warexcl;
	private final WarResource warResource;
	private final List<WarRandomFileCommand> warfiles;

	public WarBuildCommand(WarCommand parent, StructureHelper files, WarResource warResource, String targetName, List<PendingResource> warlibs, List<WarRandomFileCommand> warfiles, List<Pattern> warexcl) {
		this.parent = parent;
		this.files = files;
		this.warResource = warResource;
		this.warlibs = warlibs;
		this.warfiles = warfiles;
		this.warexcl = warexcl;
		this.warfile = new File(files.getOutputDir(), targetName);
	}

	public void add(File file) {
		dirsToJar.add(file);
	}
	
	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		File tmp = files.getOutput("WebRoot");
		File tmpClasses = files.getOutput("WebRoot/WEB-INF/classes");
		File tmpLib = files.getOutput("WebRoot/WEB-INF/lib");
		FileUtils.cleanDirectory(tmp);
		FileUtils.assertDirectory(tmp);

		// Figure out dependent projects ...
		List<Strategem> str = new ArrayList<Strategem>();
		str.add(parent);
		for (PendingResource r : warlibs)
		{
			BuildResource br = cxt.getPendingResource(r);
			copyLib(tmpLib, br);
			if (br.getBuiltBy() != null)
				str.add(br.getBuiltBy());
		}
		for (WarRandomFileCommand wrf : warfiles)
		{
			File from = wrf.getFrom(cxt);
			File to = wrf.getTo(cxt, tmp);
			if (!from.isFile())
				throw new QuickBuildException("The file " + from + " has not been created; needed by war command");
			FileUtils.copyAssertingDirs(from, to);
		}
		for (Strategem s : str)
		{
			for (BuildResource r : cxt.getDependencies(s))
			{
				copyLib(tmpLib, r);
			}
		}
		
		File root = files.getRelative("WebRoot");
		File classes = files.getRelative("WebRoot/WEB-INF/classes");
		File lib = files.getRelative("WebRoot/WEB-INF/lib");
		boolean worthIt = false;
		for (File f : FileUtils.findFilesMatching(files.getRelative("WebRoot"), "*"))
		{
			if (!f.exists() || f.isDirectory())
				continue;
			if (FileUtils.isUnder(f, classes) || FileUtils.isUnder(f, lib))
				continue;
			if (!FileUtils.isUnder(f, root))
				continue;
			FileUtils.copyAssertingDirs(f, FileUtils.moveRelativeRoot(f, root, tmp));
			worthIt = true;
		}
		
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesMatching(dir, "*"))
			{
				if (f.isDirectory())
					continue;

				FileUtils.copyAssertingDirs(f, FileUtils.moveRelativeRoot(f, dir, tmpClasses));
				worthIt = true;
			}
		}


		if (!worthIt)
			return BuildStatus.SKIPPED;
		
		RunProcess proc = new RunProcess("jar");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.arg("cf");
		proc.arg(warfile.getPath());
		proc.arg("-C");
		proc.arg(tmp.getPath());
		proc.arg(".");

		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.resourceAvailable(warResource);
			return BuildStatus.SUCCESS;
		}
		return BuildStatus.BROKEN;
	}

	private void copyLib(File tmpLib, BuildResource r) {
		if (r instanceof JarResource)
		{
			for (Pattern p : warexcl)
				if (p.matcher(r.getPath().getName().toLowerCase()).matches())
					return;
			FileUtils.copyAssertingDirs(r.getPath(), new File(tmpLib, r.getPath().getName()));
		}
	}

	@Override
	public String toString() {
		return "Create WAR: " + warResource;
	}
}

/*

META-INF/
META-INF/MANIFEST.MF

index.html

images/
images/DivingIn.jpg
scripts/
scripts/clientvalidation.js
styles/
styles/questions.css

WEB-INF/
WEB-INF/web.xml
WEB-INF/applicationContext.xml
WEB-INF/breckenridge-common.xml
WEB-INF/breckenridge-hsqldb.xml
WEB-INF/breckenridge-mysql.xml
WEB-INF/breckenridge-servlet.xml
WEB-INF/breckenridge-sqlServer.xml
WEB-INF/breckenridge-sqlServer2005.xml
WEB-INF/defaultHibernateConfigurator.xml

WEB-INF/freemarker/
WEB-INF/freemarker/ParticipantSetupPage.ftl
WEB-INF/freemarker/components/
WEB-INF/freemarker/macros/


WEB-INF/lib/
WEB-INF/lib/commons-io-1.4-sources.jar
WEB-INF/classes/org/breckenridge/businesslogic/
WEB-INF/classes/org/breckenridge/businesslogic/ActivitySurvey.class
WEB-INF/classes/breckenridgeConfigurator_sicklebill.properties
WEB-INF/classes/log4j.xml

*/