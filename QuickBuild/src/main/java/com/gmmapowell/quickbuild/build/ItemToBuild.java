package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gmmapowell.git.GitRecord;
import com.gmmapowell.quickbuild.app.BuildOutput;
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

	public ItemToBuild(Tactic tactic, String id, String label) {
		this.needsBuild = BuildStatus.CLEAN;
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
	public void export(BuildOutput output, ResourceManager rm, boolean verbose) {
		List<? extends Tactic> tactics = tactic.belongsTo().tactics();
		Tactic last = tactics.get(tactics.size()-1);
		if (last != tactic)
			return;
		if (!verbose)
			output.complete(tactic.belongsTo().identifier());
		rm.exportAll(tactic.belongsTo());
	}

	public boolean considerAutoSkipping(BuildContext cxt) {
		return (tactic instanceof CanBeSkipped && ((CanBeSkipped)tactic).skipMe(cxt));
	}

	public void announce(BuildOutput output, boolean verbose, int currentTactic, BuildStatus showStatus) {
		if (verbose) {
			StringBuilder sb = new StringBuilder();
			if (showStatus == BuildStatus.NOTAPPLICABLE)
				sb.append("v");
			else if (showStatus == BuildStatus.NOTCRITICAL)
				sb.append(".");
			else if (showStatus == BuildStatus.BROKEN_DEPENDENCIES)
				sb.append("<");
			else if (showStatus == BuildStatus.SKIPPED) // defer now, do later ...
				sb.append("-");
			else if (showStatus == BuildStatus.SUCCESS) // normal build
				sb.append("*");
			else if (showStatus == BuildStatus.RETRY) // just literally failed ... retrying
				sb.append("!");
			else if (showStatus == BuildStatus.CLEAN) // is clean, that's OK
				sb.append(" ");
			else
				throw new RuntimeException("Cannot handle status " + showStatus);

			sb.append(" " + StringUtil.rjdigits(currentTactic+1, 3) + ". " + tactic);
			String cmd = tactic.toString();
			for (int i=0;i<cmd.length();i++)
				if (!Character.isLetter(cmd.charAt(i))) {
					cmd = cmd.substring(0, i);
					break;
				}
			output.startBuildStep(cmd, sb.toString());
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

	public boolean hasUnbuiltDependencies(BuildOrder bo) {
		return bo.hasUnbuiltDependencies(this);
	}
}
