package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zinutils.exceptions.UtilException;
import org.zinutils.git.GitHelper;
import org.zinutils.git.GitRecord;

import com.gmmapowell.quickbuild.build.java.JavaSourceDirResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;

import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;
import org.zinutils.utils.PrettyPrinter;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

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
	private final DependencyManager dependencies;
	private final Set<BuildResource> dirtyResources = new HashSet<BuildResource>();
	private final Set<GitRecord> ubtxs = new HashSet<GitRecord>();

	private Set<Tactic> completedTactics = new HashSet<Tactic>();

	public BuildOrder(BuildContext cxt, DependencyManager manager, boolean buildAll, boolean debug)
	{
		this.cxt = cxt;
		dependencies = manager;
		this.buildAll = buildAll;
		this.debug = debug;
		buildOrderFile = cxt.getCacheFile("buildOrder.xml");
	}	

	public void clear() {
		mapping.clear();
		toBuild.clear();
		well.clear();
	}
	
	public void buildAll() {
		buildAll = true;
	}

	public void knowAbout(Tactic t) {
		ItemToBuild itb = new ItemToBuild(t, t.identifier(), t.identifier());
		mapping.put(t.identifier(), itb);
		well.add(itb);
	}

	void loadBuildOrderCache() {
		if (!buildOrderFile.canRead())
		{
			return;
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
			toBuild.addAll(well);
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
				dirtyResources.add(br);
		}
		final List<ItemToBuild> itbQueue = new ArrayList<ItemToBuild>();
		for (ItemToBuild itb : toBuild)
			itbQueue.add(itb);
		for (ItemToBuild itb : well)
			itbQueue.add(itb);
		for (ItemToBuild itb : itbQueue)
			figureDirtyness(manager, itb);
	}

	public void figureDirtyness(DependencyManager manager, ItemToBuild itb) {
		if (itb == null || itb.tactic == null)
			return;
		boolean isDirty = false;
		if (buildAll)
		{
			if (debug)
				cxt.output.println("Marking " + itb + " dirty due to --build-all");
			isDirty = true;
		}
		boolean wasDirty = isDirty;
		OrderedFileList files = itb.tactic.sourceFiles();
		if (files == null && itb.getDependencies(manager).isEmpty() && itb.getProcessDependencies().isEmpty())
		{
			isDirty = true;
			if (!wasDirty && debug)
				cxt.output.println("Marking " + itb + " dirty due to NULL file list");
		}
		else
		{
			GitRecord gittx = GitHelper.checkFiles(itb.isClean() && !buildAll, files, cxt.getGitCacheFile(itb.name(), ""));
			itb.addGitTx(gittx);
			isDirty |= gittx.isDirty();
			if (!wasDirty && isDirty && debug)
				cxt.output.println("Marking " + itb + " dirty due to git hash-object");
		}
		if (!isDirty)
		{
			File f = cxt.getGitCacheFile(itb.name(), "-gitid");
			if (f.exists()) {
				String prev = FileUtils.readFile(f);
				String head = System.getenv("BUILD_VCS_NUMBER");
				if (head == null || head.trim().length() == 0)
					head = GitHelper.currentHead();
				if (head == null || prev == null || !head.equals(prev))
					isDirty = true;
			}
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
				else if (dirtyResources.contains(wb) || (wb.getBuiltBy() != null && !mapping.get(wb.getBuiltBy().identifier()).isClean())) {
					if (debug)
						cxt.output.println("Marking " + itb + " dirty because " + wb.compareAs() + " is dirty or being dirtied");
					isDirty = true;
				}
			}
		}
		if (!isDirty)
		{
			for (Tactic d : itb.getProcessDependencies())
			{
				if (!mapping.get(d.identifier()).isClean())
				{
					isDirty = true;
					if (debug)
						cxt.output.println("Marking " + itb + " dirty due to " + d + " is dirty");
				}
			}
		}
		if (isDirty || buildAll)
		{
			itb.markDirty();
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
		if (!cxt.grandFallacy) {
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

	public void completeTactic(Tactic tactic) {
		completedTactics.add(tactic);
	}
	
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
		for (Tactic i : completedTactics)
			if (i == t)
				return true;
		return false;
	}

	public void reject(Tactic t, int breakAt) {
		if (!mapping.containsKey(t.identifier()))
			throw new UtilException("Cannot reject non-existent " + t.identifier() + " have " + mapping.keySet());
		int rc;
		if (breakAt != -1)
			rc = well.size(); // place at bottom of well
		else
			rc = 0; // place in well in basically the same order they currently are in the build order
		for (int i=0;i<toBuild.size();i++) {
			ItemToBuild itb = toBuild.get(i);
			boolean reject = false;
			if (itb.tactic == t) {
				itb.sentToBottomAt = breakAt;
				reject = true;
			}
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
