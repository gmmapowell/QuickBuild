package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
public class BuildOrder implements Iterable<ItemToBuild> {
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
	private final int nthreads;

	public BuildOrder(BuildContext cxt, boolean buildAll, boolean debug)
	{
		this.cxt = cxt;
		this.buildAll = buildAll;
		this.debug = debug;
		this.nthreads = cxt.getNumThreads();
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
			ItemToBuild itb = new ItemToBuild(t, t.identifier(), t.identifier());
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
		int i=1;
		for (ItemToBuild b : toBuild)
		{
			pp.append(i +". ");
			b.print(pp);
			if (b.drift() > 0)
				pp.append(" (drift " + b.drift() +")");
			pp.requireNewline();
			i++;
		}
		return pp.toString();
	}

	public void figureDirtyness(final DependencyManager manager) {
		for (BuildResource br : manager.unBuilt())
		{
			// This is an out-of-band hack and should be cleaned up - GP 2012-11-10
			if (br instanceof JavaSourceDirResource)
				continue;
			File f = br.getPath();
			OrderedFileList ofl = new OrderedFileList(f);
//			System.out.println("Considering file " + f + " for " + br);
			String relpath = f.getPath();
			try {
				relpath = FileUtils.makeRelative(f).getPath();
			} catch (Exception ex) { /* probably not relative */ }
			GitRecord ubtx = GitHelper.checkFiles(!buildAll, ofl, cxt.getGitCacheFile("Unbuilt_"+relpath.replace("/", "_"), ""));
			ubtxs.add(ubtx);
			if (ubtx.isDirty())
				dirtyUnbuilt.add(br);
		}
		final List<ItemToBuild> itbQueue = new ArrayList<ItemToBuild>();
		for (ItemToBuild itb : toBuild)
			itbQueue.add(itb);
		for (ItemToBuild itb : well)
			itbQueue.add(itb);
		if (nthreads < 2 || itbQueue.size() < 5) {
			for (ItemToBuild itb : itbQueue)
				figureDirtyness(manager, itb);
		} else {
			System.out.println("Using " + nthreads + " threads to figure dirtyness");
			int nthrs = Math.min(nthreads, itbQueue.size());
			Thread[] thrs = new Thread[nthrs];
			for (int i=0;i<nthrs;i++) {
				final int j = i;
				thrs[i] = new Thread() {
					public void run() {
						while (true) {
							ItemToBuild itb;
							synchronized (itbQueue) {
								if (itbQueue.isEmpty())
									break;
								itb = itbQueue.remove(0);
							}
							System.out.println("Thread " + j + " took item " + itb);
							figureDirtyness(manager, itb);
							System.out.println("       " + j + " done with " + itb);
						}
						System.out.println("  Done: " + j);
					}
				};
				thrs[i].start();
			}
			for (int i=0;i<nthrs;i++) {
				while (thrs[i].isAlive())
					try {
						thrs[i].join();
					} catch (InterruptedException ex) {
						; // whatever
					}
			}
		}
	}

	public void figureDirtyness(DependencyManager manager, ItemToBuild itb) {
		if (itb == null || itb.tactic == null)
			return;
//		System.out.println("Considering " + itb);
		boolean isDirty = false;
		if (buildAll)
		{
			if (debug)
				cxt.output.println("Marking " + itb + " dirty due to --build-all");
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
				cxt.output.println("Marking " + itb + " dirty due to NULL file list");
		}
		else if (files != null)
		{
			GitRecord gittx = GitHelper.checkFiles(itb.isClean() && !buildAll, files, cxt.getGitCacheFile(itb.name(), ""));
			itb.addGitTx(gittx);
			isDirty |= gittx.isDirty();
			if (!wasDirty && isDirty && debug)
				cxt.output.println("Marking " + itb + " dirty due to git hash-object");
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
						cxt.output.println("Marking " + itb + " dirty because " + wb.compareAs() + " does not have a file output");
					isDirty = true;
				}
				else if (!wb.getPath().exists())
				{
					if (debug)
						cxt.output.println("Marking " + itb + " dirty because " + wb.compareAs() + " does not exist");
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
							cxt.output.println("Marking " + itb + " dirty due to library " + d + " is dirty");
					}
				}
				else if (!mapping.get(d.getBuiltBy().identifier()).isClean())
				{
					isDirty = true;
					if (debug)
						cxt.output.println("Marking " + itb + " dirty due to " + d + " is dirty");
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
				cxt.output.println("Marking " + itb + " locally dirty due to git hash-object on ancillaries");
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
				cxt.output.println("Considering " + itb.id);
			}
			boolean hasDependency = hasUnbuiltDependencies(itb);
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
		cxt.output.openBlock("noWell");
		cxt.output.println("There is nothing in the well that can be added!");
		cxt.output.println("Well contents:");
		for (ItemToBuild p : well)
		{
			cxt.output.println("  " + p.name() + " drift: " + p.drift());
			boolean hasDependency = false;
			for (BuildResource pr : p.getDependencies(dependencies))
			{
				if (!isBuilt(pr))
				{
					cxt.output.println("    Rejecting because " + pr + " is not built");
					hasDependency = true;
					continue;
				}
				else if (hasDependency)
					continue;
			}
		}
		cxt.output.closeBlock("noWell");
		throw new UtilException("There is no way to build everything");
	}

	boolean hasUnbuiltDependencies(ItemToBuild itb) {
		boolean hasDependency = false;
		for (Tactic t : itb.getProcessDependencies()) {
			if (!isBuilt(t)) {
				hasDependency = true;
				if (debug)
					cxt.output.println("  Rejecting because " + t + " is not built");
			}
		}
		for (BuildResource pr : itb.getDependencies(dependencies))
		{
			if (!isBuilt(pr))
			{
				if (debug)
					cxt.output.println("  Rejecting because " + pr + " is not built");
				hasDependency = true;
				continue;
			}
			else if (hasDependency)
				continue;
			if (debug)
				cxt.output.println("  Dependency " + pr + " has been built");
		}
		return hasDependency;
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
		int rc = 0; // place in well in the same order they currently are in the build order
		for (int i=0;i<toBuild.size();i++) {
			ItemToBuild itb = toBuild.get(i);
			boolean reject = itb.tactic == t;
			if (!reject) {
				Iterable<BuildResource> deps = dependencies.getDependencies(itb.tactic);
				for (BuildResource br : deps) {
					if (br instanceof ProcessResource && t == ((ProcessResource)br).getTactic())
						reject = true;
				}
			}
			if (reject) {
				if (debug)
					cxt.output.println("Rejecting tactic " + t);
				toBuild.remove(itb);
				well.add(rc++, itb);
				i--;
			}
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

	@Override
	public Iterator<ItemToBuild> iterator() {
		return toBuild.iterator();
	}

	public void cleanFile() {
		buildOrderFile.delete();		
	}
}
