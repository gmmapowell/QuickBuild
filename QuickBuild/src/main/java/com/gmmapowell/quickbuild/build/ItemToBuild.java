package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gmmapowell.git.GitRecord;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;
import com.gmmapowell.utils.StringUtil;

/**
 * The purpose of this class is to communicate between BuildOrder (which knows
 * what to build) and BuildContext (which knows how to build), what should be
 * built.
 * 
 * <p>
 * &copy; 2011 Gareth Powell. All rights reserved.
 * 
 * @author Gareth Powell
 * 
 */
public class ItemToBuild {
	public BuildStatus needsBuild;
	public final Tactic tactic;
	public final String id;
	public final String label;
	private final List<GitRecord> gittxs = new ArrayList<GitRecord>();
	private int drift;

	public ItemToBuild(BuildStatus needsBuild, Tactic tactic, String id, String label) {
		this.needsBuild = needsBuild;
		this.tactic = tactic;
		this.id = id;
		this.label = label;
		if (tactic instanceof FloatToEnd)
			this.drift = ((FloatToEnd)tactic).priority();
		else if (tactic.belongsTo() instanceof FloatToEnd)
			this.drift = ((FloatToEnd)tactic.belongsTo()).priority();
	}

	public void markDirty() {
		needsBuild = BuildStatus.SUCCESS;
	}

	public void markDirtyLocally() {
		needsBuild = BuildStatus.SUCCESS;
	}

	public int compareTo(ItemToBuild other) {
		return id.compareTo(other.id);
	}

	public String name() {
		return tactic.identifier();
	}

	/** Export anything that is completed by this operation */
	public void export(ResourceManager rm) {
		List<? extends Tactic> tactics = tactic.belongsTo().tactics();
		Tactic last = tactics.get(tactics.size()-1);
		if (last != tactic)
			return;
		System.out.println("       Completing " + tactic.belongsTo().identifier());
		rm.exportAll(tactic.belongsTo());
	}

	public void announce(boolean verbose, int currentTactic, BuildStatus showStatus) {
		if (verbose) {
			if (showStatus == BuildStatus.NOTAPPLICABLE)
				System.out.print("v");
			else if (showStatus == BuildStatus.NOTCRITICAL)
				System.out.print(".");
			else if (showStatus == BuildStatus.BROKEN_DEPENDENCIES)
				System.out.print("<");
			else if (showStatus == BuildStatus.SKIPPED) // defer now, do later ...
				System.out.print("-");
			else if (showStatus == BuildStatus.SUCCESS) // normal build
				System.out.print("*");
			else if (showStatus == BuildStatus.RETRY) // just literally failed ... retrying
				System.out.print("!");
			else if (showStatus == BuildStatus.CLEAN) // is clean, that's OK
				System.out.print(" ");
			else
				throw new RuntimeException("Cannot handle status " + showStatus);

			System.out.println(" " + StringUtil.rjdigits(currentTactic+1, 3) + ". " + tactic);
		}
	}


	public void addGitTx(GitRecord gittx) {
		gittxs.add(gittx);
	}

	public void commitAll() {
		for (GitRecord gr : gittxs )
			gr.commit();
	}

	public void fail() {
		for (GitRecord gr : gittxs )
			gr.setError();
	}

	public void revert() {
		for (GitRecord gr : gittxs)
			gr.revert();
	}

	public Set<Tactic> getProcessDependencies() {
		return tactic.getProcessDependencies();
	}

	public Iterable<BuildResource> getDependencies(DependencyManager dependencies) {
		Iterable<BuildResource> ret = dependencies.getDependencies(tactic);
		return ret;
	}

	public void setDrift(int drift) {
		this.drift = drift;
	}

	public int drift() {
		return drift;
	}

	public void print(PrettyPrinter pp) {
		pp.append(this);
	}
	
	@Override
	public String toString() {
		return tactic.toString();
	}

	public boolean isClean() {
		return needsBuild == BuildStatus.CLEAN;
	}
}
