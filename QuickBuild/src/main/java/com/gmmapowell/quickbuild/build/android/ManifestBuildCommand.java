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
import com.gmmapowell.collections.ListMap;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;
import com.gmmapowell.xml.XMLNamespace;

public class ManifestBuildCommand extends AbstractTactic {

	public class IntentFilterOpts {
		public String action;
		public String[] categories;
	}

	private final AndroidContext acxt;
	private final File manifestFile;
	private final boolean justEnough;
	private final File srcdir;
	private final File bindir;

	public ManifestBuildCommand(AndroidCommand parent, AndroidContext acxt, File manifest, boolean justEnough, File srcdir, File bindir) {
		super(parent);
		this.acxt = acxt;
		this.manifestFile = manifest;
		this.justEnough = justEnough;
		this.srcdir = srcdir;
		this.bindir = bindir;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		AndroidCommand parent = (AndroidCommand) this.parent;
		parent.configureJRR(cxt);
		String packageName = null;
		String applClass = null;
		String mainClass = null;
		List<String> activities = new ArrayList<String>();
		List<String> services = new ArrayList<String>();
		String minpkg = null;
		Set<String> permissions = new TreeSet<String>();
		Set<String> supportedScreens = new TreeSet<String>();
		Map<String, Options> options = new HashMap<String, Options>();
		ListMap<String, IntentFilterOpts> filters = new ListMap<String, ManifestBuildCommand.IntentFilterOpts>();
		boolean debuggable = false;
		for (File f : FileUtils.findFilesUnderMatching(srcdir, "*.java"))
		{
			// Try and figure out a minimal package name for 1st pass analysis
			String pkg = FileUtils.getPackage(f);
			if (packageName == null && (minpkg == null || pkg.length() < minpkg.length()))
			{
				minpkg = pkg;
				if (showDebug)
					System.out.println("Selecting package name " + minpkg);
			}

			// Now try and find actual class file for detailed (2nd pass) analysis
			String qualifiedName = FileUtils.convertToDottedNameDroppingExtension(f);
			File clsFile = new File(bindir, FileUtils.ensureExtension(f, ".class").getPath());
			if (!clsFile.exists())
				continue;
			
			// If the file exists, look for Application & Activity classes
			ByteCodeFile bcf = new ByteCodeFile(clsFile, qualifiedName);
			boolean appOrAct = false;
			boolean appActOrServ = false;
			if (bcf.nestedExtendsClass(parent.jrr, "android.app.Application") && bcf.isConcrete())
			{
				if (showDebug)
					System.out.println("Found application class " + bcf.getName());
				if (applClass != null)
				{
					System.out.println("More than one Application class");
					return BuildStatus.BROKEN;
				}
				packageName = FileUtils.getPackage(f);
				applClass = qualifiedName;
				appOrAct = true;
			}
			else if (bcf.nestedExtendsClass(parent.jrr, "android.app.Activity") && bcf.isConcrete())
			{
				if (showDebug)
					System.out.println("Found activity class " + bcf.getName());
				activities.add(qualifiedName);
				Annotation mainAnn = bcf.getClassAnnotation("com.gmmapowell.android.MainActivity");
				if (mainAnn != null)
				{
					if (showDebug)
						System.out.println("Found main activity class " + bcf.getName());
					if (mainClass != null)
					{
						System.out.println("More than one class annotated @MainActivity");
						return BuildStatus.BROKEN;
					}
					mainClass = qualifiedName;
				}
				appOrAct = true;
			}
			else if (bcf.extendsClass("android.app.Service") || bcf.extendsClass("android.app.IntentService"))
			{
				services.add(qualifiedName);
				Options opts = new Options();
				Annotation ann = bcf.getClassAnnotation("com.gmmapowell.android.Service");
				if (ann != null)
					opts.name = ann.getArg("value").asString();
				else
					opts.name = qualifiedName.replace(packageName+".", "").replaceAll("service", "");
				options.put(qualifiedName, opts);
				appActOrServ = true;
			}
		
			if (appOrAct)
			{
				appActOrServ = true;
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
			
			if (appActOrServ)
			{
				Annotation intentFilterAnn = bcf.getClassAnnotation("com.gmmapowell.android.IntentFilter");
				if (intentFilterAnn != null)
				{
					IntentFilterOpts opts = new IntentFilterOpts();
					filters.add(qualifiedName, opts);
					AnnotationValue tmpA = intentFilterAnn.getArg("action");
					if (tmpA != null)
						opts.action = tmpA.asString();
					AnnotationValue tmp1 = intentFilterAnn.getArg("category");
					if (tmp1 != null)
					{
						AnnotationValue[] tmp = tmp1.asArray();
						opts.categories = new String[tmp.length];
						for (int i=0;i<tmp.length;i++)
							opts.categories[i] = tmp[i].asString();
					}
				}				
			}
			
			// Any class can request a permission
			Annotation usesPerm = bcf.getClassAnnotation("com.gmmapowell.android.UsesPermission");
			if (usesPerm != null)
			{
				AnnotationValue arg = usesPerm.getArg("value");
				if (arg.isString())
					permissions.add(arg.asString());
				else if (arg.isArray())
				{
					for (AnnotationValue av : arg.asArray())
						permissions.add(av.asString());
				}
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
				addOtherIntents(activity, act, filters, android);
				if (actOpts.label != null)
					act.setAttribute(android.attr("label"), actOpts.label);
				if (actOpts.theme != null)
					act.setAttribute(android.attr("theme"), "@style/" + actOpts.theme);
			}
			
			for (String service : services)
			{
				XMLElement s = appl.addElement("service");
				s.setAttribute(android.attr("name"), service.replace(packageName, ""));
				addOtherIntents(service, s, filters, android);
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

	private void addOtherIntents(String name, XMLElement node, ListMap<String, IntentFilterOpts> filters, XMLNamespace android) {
		if (!filters.contains(name))
			return;
		
		for (IntentFilterOpts o : filters.get(name))
		{
			XMLElement filter = node.addElement("intent-filter");
			if (o.action != null && o.action.trim().length() > 0)
				filter.addElement("action").setAttribute(android.attr("name"), o.action);
			
			if (o.categories != null)
			{
				for (String s : o.categories)
				{
					filter.addElement("category").setAttribute(android.attr("name"), s);
				}
			}
		}
	}

	private String stripPkg(String packageName, String applClass) {
		return applClass.replace(packageName, "");
	}

	@Override
	public String toString() {
		return (justEnough?"maninit: ":"manbuild: ") + FileUtils.makeRelative(manifestFile);
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, justEnough?"maninit":"manifest");
	}

	public static class Options {
		public String name;
		public String icon;
		public String label;
		public String theme;
	}
}
