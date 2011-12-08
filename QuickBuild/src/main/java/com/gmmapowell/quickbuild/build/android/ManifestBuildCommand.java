package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.bytecode.Annotation;
import com.gmmapowell.bytecode.AnnotationValue;
import com.gmmapowell.bytecode.ByteCodeFile;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;
import com.gmmapowell.xml.XMLNamespace;

public class ManifestBuildCommand implements Tactic {

	private final AndroidContext acxt;
	private final File manifestFile;
	private final AndroidCommand parent;
	private final boolean justEnough;
	private final File srcdir;
	private final File bindir;

	public ManifestBuildCommand(AndroidCommand parent, AndroidContext acxt, File manifest, boolean justEnough, File srcdir, File bindir) {
		this.parent = parent;
		this.acxt = acxt;
		this.manifestFile = manifest;
		this.justEnough = justEnough;
		this.srcdir = srcdir;
		this.bindir = bindir;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		String packageName = null;
		String applClass = null;
		String mainClass = null;
		List<String> activities = new ArrayList<String>();
		String minpkg = null;
		Set<String> permissions = new TreeSet<String>();
		Set<String> supportedScreens = new TreeSet<String>();
		Map<String, Options> options = new HashMap<String, Options>();
		boolean debuggable = false;
		for (File f : FileUtils.findFilesUnderMatching(srcdir, "*.java"))
		{
			// Try and figure out a minimal package name for 1st pass analysis
			String pkg = FileUtils.getPackage(f);
			if (packageName == null && (minpkg == null || pkg.length() < minpkg.length()))
				minpkg = pkg;

			// Now try and find actual class file for detailed (2nd pass) analysis
			String qualifiedName = FileUtils.convertToDottedNameDroppingExtension(f);
			File clsFile = new File(bindir, FileUtils.ensureExtension(f, ".class").getPath());
			if (!clsFile.exists())
				continue;
			
			// If the file exists, look for Application & Activity classes
			ByteCodeFile bcf = new ByteCodeFile(clsFile, qualifiedName);
			boolean appOrAct = false;
			if (bcf.extendsClass("android.app.Application"))
			{
				if (applClass != null)
				{
					System.out.println("More than one Application class");
					return BuildStatus.BROKEN;
				}
				packageName = FileUtils.getPackage(f);
				applClass = qualifiedName;
				appOrAct = true;
			}
			// TODO: this should really look all the way up the hierarchy until it finds "android.app.Activity"
			// but that's too hard right now ... 
			else if (bcf.extendsClass("android.app.Activity") || bcf.extendsClass("android.preference.PreferenceActivity"))
			{
				activities.add(qualifiedName);
				Annotation mainAnn = bcf.getClassAnnotation("com.gmmapowell.android.MainActivity");
				if (mainAnn != null)
				{
					if (mainClass != null)
					{
						System.out.println("More than one class annotated @MainActivity");
						return BuildStatus.BROKEN;
					}
					mainClass = qualifiedName;
				}
				appOrAct = true;
			}
			
			if (appOrAct)
			{
				Options opts = new Options();
				options.put(qualifiedName, opts);
				Annotation appLabelAnn = bcf.getClassAnnotation("com.gmmapowell.android.Label");
				if (appLabelAnn != null)
					opts.label = appLabelAnn.getArg("value").asString();
				Annotation theme = bcf.getClassAnnotation("com.gmmapowell.android.Theme");
				if (theme != null)
					opts.theme = theme.getArg("value").asString();
				Annotation icon = bcf.getClassAnnotation("com.gmmapowell.android.Icon");
				if (icon != null)
					opts.icon = icon.getArg("value").asString();
			}
			
			// Any class can request a permission
			Annotation usesPerm = bcf.getClassAnnotation("com.gmmapowell.android.UsesPermission");
			if (usesPerm != null)
			{
				permissions.add(usesPerm.getArg("value").asString());
			}
			
			Annotation screens = bcf.getClassAnnotation("com.gmmapowell.android.SupportsScreen");
			if (screens != null)
			{
				for (AnnotationValue arg : screens.getArg("value").asArray())
				{
					supportedScreens.add(arg.asString());
				}
			}

			Annotation debug = bcf.getClassAnnotation("com.gmmapowell.android.Debuggable");
			if (debug != null)
			{
				debuggable = true;
			}
			
		}

		if (packageName == null)
			packageName = minpkg;
		if (packageName == null)
		{
			System.out.println("No package name could be found");
			return BuildStatus.BROKEN;
		}
		
		if (mainClass == null && !justEnough)
		{
			System.out.println("WARNING: There is no activity annotated with @MainActivity");
		}
		
		XML manifest = XML.create("1.0", "manifest");
		XMLElement top = manifest.top();
		XMLNamespace android = manifest.namespace("android", "http://schemas.android.com/apk/res/android");
		top.setAttribute("package", packageName);
		// TODO: if we want to customize these, these should be options on the build command, I think
		top.setAttribute(android.attr("versionName"), "1.0");
		top.setAttribute(android.attr("versionCode"), "1");
		
		if (!justEnough)
		{
			for (String s : permissions)
			{
				top.addElement("uses-permission").setAttribute(android.attr("name"), s);
			}
			
			XMLElement appl = top.addElement("application");
			if (applClass != null)
			{
				Options appOpts = options.get(applClass);
				appl.setAttribute(android.attr("name"), stripPkg(packageName, applClass));
				if (appOpts.label != null)
					appl.setAttribute(android.attr("label"), appOpts.label);
				if (appOpts.theme != null)
					appl.setAttribute(android.attr("theme"), "@style/" + appOpts.theme);
				if (appOpts.icon != null)
					appl.setAttribute(android.attr("icon"), "@drawable/" + appOpts.icon);
			}
			else if (mainClass != null)
			{
				Options appOpts = options.get(mainClass);
				if (appOpts.theme != null)
					appl.setAttribute(android.attr("theme"), "@style/" + appOpts.theme);
				if (appOpts.icon != null)
					appl.setAttribute(android.attr("icon"), "@drawable/" + appOpts.icon);
			}
			if (debuggable)
				appl.setAttribute(android.attr("debuggable"), "true");
			
			for (String activity : activities)
			{
				Options actOpts = options.get(activity);
				XMLElement act = appl.addElement("activity");
				act.setAttribute(android.attr("name"), activity.replace(packageName, ""));
				if (activity.equals(mainClass))
				{
					XMLElement filter = act.addElement("intent-filter");
					filter.addElement("action").setAttribute(android.attr("name"), "android.intent.action.MAIN");
					filter.addElement("category").setAttribute(android.attr("name"), "android.intent.category.LAUNCHER");
				}
				if (actOpts.label != null)
					act.setAttribute(android.attr("label"), actOpts.label);
				if (actOpts.theme != null)
					act.setAttribute(android.attr("theme"), "@style/" + actOpts.theme);
			}
			
			XMLElement usesSdk = top.addElement("uses-sdk");
			// This probably should be customizable
			usesSdk.setAttribute(android.attr("minSdkVersion"), "4");
			usesSdk.setAttribute(android.attr("targetSdkVersion"), acxt.getAndroidPlatform().replace("android-", ""));
			
			if (!supportedScreens.isEmpty())
			{
				XMLElement supported = top.addElement("supports-screens");
				for (String s : supportedScreens)
				{
					String type = s.toLowerCase();
					if (!type.equals("resizeable"))
						type = type + "Screens";
					supported.setAttribute(android.attr(type), "true");
				}
			}
		}
		manifest.write(manifestFile);
		return BuildStatus.SUCCESS;
	}

	private String stripPkg(String packageName, String applClass) {
		return applClass.replace(packageName, "");
	}

	@Override
	public String toString() {
		return (justEnough?"maninit: ":"manbuild: ") + FileUtils.makeRelative(manifestFile);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, justEnough?"maninit":"manifest");
	}

	public static class Options {
		public String icon;
		public String label;
		public String theme;
	}
}
