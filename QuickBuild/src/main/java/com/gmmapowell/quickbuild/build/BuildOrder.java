package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.git.GitHelper;
import com.gmmapowell.git.GitRecord;
import com.gmmapowell.quickbuild.build.java.JavaSourceDirResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.PrettyPrinter;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;

/**
 * The idea of moving this into its own file is so that it can have a richer structure than it had
 * before while still presenting a sane, untangled interface to the rest of the world.
 * <p>
 * All users _really_ care about is "what tactic should I do next?".  However, in order to play the
 * game they also have to declare "discovered" dependencies and whether or not to "advance".
 * <p>
 * Internally, this class needs to keep track of strategems, free-floating tactics, and things that
 * want to move around.
 * <p>
 * It also needs to keep track of strategem-strategem dependencies, although the "why" is handled in
 * the Dependency Manager.
 * <p>
 * Thus this class needs a static strategem/tactic ordering graph AND a notion of "current execution point"
 *
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class BuildOrder {
	// We are going to track everything based on name, so we need to map name to strategem
	private final Map<String, ItemToBuild> mapping = new HashMap<String, ItemToBuild>();
	private final List<ItemToBuild> well = new ArrayList<ItemToBuild>();
	private final List<ItemToBuild> toBuild = new ArrayList<ItemToBuild>();

	private final File buildOrderFile;
	private boolean buildAll;
	private final boolean debug;

	private final BuildContext cxt;
	private DependencyManager dependencies;
	private final Set<BuildResource> dirtyUnbuilt = new HashSet<BuildResource>();
	private final Set<GitRecord> ubtxs = new HashSet<GitRecord>();

	public BuildOrder(BuildContext cxt, boolean buildAll, boolean debug)
	{
		this.cxt = cxt;
		this.buildAll = buildAll;
		this.debug = debug;
		buildOrderFile = cxt.getCacheFile("buildOrder.xml");
	}	

	public void dependencyManager(DependencyManager manager) {
		this.dependencies = manager;
	}

	public void clear() {
		mapping.clear();
		toBuild.clear();
		well.clear();
	}
	
	public void buildAll() {
		buildAll = true;
	}

	public void knowAbout(Strategem s) {
		for (Tactic t : s.tactics()) {
			ItemToBuild itb = new ItemToBuild(BuildStatus.CLEAN, t, t.identifier(), t.identifier());
			mapping.put(t.identifier(), itb);
			well.add(itb);
		}
	}

	void loadBuildOrderCache() {
		if (!buildOrderFile.canRead())
		{
			throw new QuickBuildCacheException("There was no build order cache", null);
		}
		try
		{
			final XML input = XML.fromFile(buildOrderFile);
			for (XMLElement elt : input.top().elementChildren())
			{
				int drift = 0;
				if (elt.hasAttribute("drift"))
					drift  = Integer.parseInt(elt.get("drift"));
				ItemToBuild itb = mapping.get(elt.get("name"));
				if (itb == null)
					continue;
				itb.setDrift(drift);
				toBuild.add(itb);
				well.remove(itb);
			}
		} catch (Exception ex)
		{
			buildOrderFile.delete();
			throw new QuickBuildCacheException("Could not parse build order cache", ex);
		}
	}

	public void saveBuildOrder() {
		final XML output = XML.create("1.0", "BuildOrder");
		for (ItemToBuild itb : toBuild)
		{
			XMLElement elt = output.addElement("ItemToBuild");
			elt.setAttribute("name", itb.id);
			if (itb.drift() != 0)
				elt.setAttribute("drift", "" + itb.drift());
		}
		FileUtils.assertDirectory(buildOrderFile.getParentFile());
		output.write(buildOrderFile);
	}

	public static String tacticIdentifier(Strategem parent, String suffix) {
		String ret = parent.identifier();
		if (!ret.endsWith("]"))
			throw new RuntimeException("Identifiers should end with ]: " + ret);
		return ret.substring(0, ret.length()-1) + "-" + suffix + "]";
	}

	public String printOut(boolean withTactics) {
		PrettyPrinter pp = new PrettyPrinter();
		pp.indentWidth(2);
		pp.indentMore();
		int i=0;
		for (ItemToBuild b : toBuild)
		{
			pp.append("Band " + i);
			if (b.drift() > 0)
				pp.append(" (drift " + b.drift() +")");
			pp.indentMore();
			b.print(pp);
			pp.indentLess();
			i++;
		}
		return pp.toString();
	}

	public void figureDirtyness(DependencyManager manager) {
		for (BuildResource br : manager.unBuilt())
		{
			// This is an out-of-band hack and should be cleaned up - GP 2012-11-10
			if (br instanceof JavaSourceDirResource)
				continue;
			File f = br.getPath();
			OrderedFileList ofl = new OrderedFileList(f);
//			System.out.println("Considering file " + f + " for " + br);
			GitRecord ubtx = GitHelper.checkFiles(!buildAll, ofl, cxt.getGitCacheFile("Unbuilt_"+FileUtils.makeRelative(f).getPath().replace("/", "_"), ""));
			ubtxs.add(ubtx);
			if (ubtx.isDirty())
				dirtyUnbuilt.add(br);
		}
		for (ItemToBuild itb : toBuild)
			figureDirtyness(manager, itb);
		for (ItemToBuild itb : well)
			figureDirtyness(manager, itb);
	}

	public void figureDirtyness(DependencyManager manager, ItemToBuild itb) {
		if (itb == null || itb.tactic == null)
			return;
//		System.out.println("Considering " + itb);
		boolean isDirty = false;
		if (buildAll)
		{
			if (debug)
				System.out.println("Marking " + itb + " dirty due to --build-all");
			isDirty = true;
		}
		boolean wasDirty = isDirty;
		OrderedFileList files = itb.tactic.belongsTo().sourceFiles();
		OrderedFileList ancillaries = null;
		if (itb.tactic.belongsTo() instanceof HasAncillaryFiles)
			ancillaries = ((HasAncillaryFiles) itb.tactic.belongsTo()).getAncillaryFiles();
		if (files == null && ancillaries == null)
		{
			isDirty = true;
			if (!wasDirty && debug)
				System.out.println("Marking " + itb + " dirty due to NULL file list");
		}
		else if (files != null)
		{
			GitRecord gittx = GitHelper.checkFiles(itb.isClean() && !buildAll, files, cxt.getGitCacheFile(itb.name(), ""));
			itb.addGitTx(gittx);
			isDirty |= gittx.isDirty();
			if (!wasDirty && isDirty && debug)
				System.out.println("Marking " + itb + " dirty due to git hash-object");
		}
		if (!isDirty)
		{
			for (BuildResource wb : itb.getDependencies(dependencies))
			{
				if (wb == null || wb instanceof ProcessResource)
					continue;
				if (wb.getPath() == null || !wb.getPath().exists())
				{
					if (debug)
						System.out.println("Marking " + itb + " dirty because " + wb.compareAs() + " does not have a file output");
					isDirty = true;
				}
				else if (!wb.getPath().exists())
				{
					if (debug)
						System.out.println("Marking " + itb + " dirty because " + wb.compareAs() + " does not exist");
					isDirty = true;
				}
			}
		}
		if (!isDirty)
		{
			for (BuildResource d : manager.getDependencies(itb.tactic))
			{
				if (d == null || d instanceof ProcessResource)
					continue;
				if (d.getBuiltBy() == null)
				{
					if (dirtyUnbuilt.contains(d))
					{
						isDirty = true;
						if (debug)
							System.out.println("Marking " + itb + " dirty due to library " + d + " is dirty");
					}
				}
				else if (!mapping.get(d.getBuiltBy().identifier()).isClean())
				{
					isDirty = true;
					if (debug)
						System.out.println("Marking " + itb + " dirty due to " + d + " is dirty");
				}
			}
		}
		boolean ancDirty = false;
		if (itb.tactic.belongsTo() instanceof HasAncillaryFiles) {
			if (ancillaries != null && !ancillaries.isEmpty())
			{
				 GitRecord ancTx = GitHelper.checkFiles(itb.isClean() && !buildAll, ancillaries, cxt.getGitCacheFile(itb.name(), ".anc"));
				 itb.addGitTx(ancTx);
				 ancDirty = ancTx.isDirty();
			}
		}
		if (isDirty || buildAll)
		{
			itb.markDirty();
		}
		else if (ancDirty) {
			if (debug)
				System.out.println("Marking " + itb + " locally dirty due to git hash-object on ancillaries");
			itb.markDirtyLocally();
		}
	}

	public ItemToBuild get(int tactic) {
		if (tactic >= toBuild.size())
		{
			if (!addFromWell())
				return null;
		}
		return toBuild.get(tactic);
	}

	private boolean addFromWell() {
		if (well.size() == 0)
			return false;
		ItemToBuild bestFit = null;
		loop:
		for (ItemToBuild itb : well)
		{
			if (debug)
			{
				System.out.println("Considering " + itb.id);
			}
			boolean hasDependency = false;
			for (Tactic t : itb.getProcessDependencies()) {
				if (!isBuilt(t)) {
					hasDependency = true;
					if (debug)
						System.out.println("  Rejecting because " + t + " is not built");
				}
			}
			for (BuildResource pr : itb.getDependencies(dependencies))
			{
				if (!isBuilt(pr))
				{
					if (debug)
						System.out.println("  Rejecting because " + pr + " is not built");
					hasDependency = true;
					continue;
				}
				else if (hasDependency)
					continue;
				if (debug)
					System.out.println("  Dependency " + pr + " has been built");
			}
			if (hasDependency)
				continue loop;
			
			if (bestFit == null || itb.drift() < bestFit.drift()) {
				bestFit = itb;
			}
		}
		if (bestFit != null) { 
			toBuild.add(bestFit);
			well.remove(bestFit);
			return true;
		}
		System.out.println("There is nothing in the well that can be added!");
		System.out.println("Well contents:");
		for (ItemToBuild p : well)
		{
			System.out.println("  " + p.name() + " drift: " + p.drift());
			boolean hasDependency = false;
			for (BuildResource pr : p.getDependencies(dependencies))
			{
				if (!isBuilt(pr))
				{
					System.out.println("    Rejecting because " + pr + " is not built");
					hasDependency = true;
					continue;
				}
				else if (hasDependency)
					continue;
			}
		}
		throw new UtilException("There is no way to build everything");
	}

	/*
	private int getDrift(BandElement be) {
		Strategem building = be.getStrat();
		return 0;
	}
	*/
	
	private boolean isBuilt(BuildResource pr) {
		if (pr instanceof PendingResource && !((PendingResource) pr).isBound())
			return false;
		Tactic t = pr.getBuiltBy();
		if (t == null)
			return true;
		return isBuilt(t);
	}

	private boolean isBuilt(Tactic t) {
		if (t == null)
			return true;
		for (ItemToBuild i : toBuild)
			if (i.tactic == t)
				return true;
		return false;
	}

	public void reject(Tactic t, boolean forever) {
		if (!mapping.containsKey(t.identifier()))
			throw new UtilException("Cannot reject non-existent " + t.identifier() + " have " + mapping.keySet());
		for (ItemToBuild itb : toBuild)
			if (itb.tactic == t) {
				if (debug)
					System.out.println("Rejecting tactic " + t);
				toBuild.remove(itb);
				if (!forever)
					well.add(0, itb);
				return;
			}
	}

	public void commitUnbuilt()
	{
		for (GitRecord gr : ubtxs)
			gr.commit();
	}

	public void revertRemainder()
	{
		for (ItemToBuild eb : this.toBuild)
			eb.revert();
	}
}
