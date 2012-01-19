package com.gmmapowell.quickbuild.build;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.git.GitRecord;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.PrettyPrinter;


public class ExecuteStrategem extends BandElement {
	private final String which;
	private boolean clean = true;
	private Strategem strat;
	private boolean needsLocalBuild = false;
	private final Set<GitRecord> gittxs = new HashSet<GitRecord>();

	public ExecuteStrategem(String which) {
		this.which = which;
	}
	
	public boolean isClean() {
		return clean;
	}

	@Override
	public boolean isCompletelyClean() {
		return clean && !needsLocalBuild;
	}

	public void markDirty() {
		clean = false;
	}
	
	public void markDirtyLocally() {
		needsLocalBuild  = true;
	}

	public String name() {
		return which;
	}
	
	public void bind(Strategem strat)
	{
		this.strat = strat;
	}

	@Override
	public int size() {
		if (strat == null)
			return 0;
		return strat.tactics().size();
	}

	@Override
	public Tactic tactic(int currentTactic) {
		return strat.tactics().get(currentTactic);
	}

	@Override
	public boolean is(String identifier) {
		return which.equals(identifier);
	}
	
	@Override
	public boolean isDeferred(Tactic tactic) {
		String id = tactic.identifier();
		for (DeferredTactic dt : deferred)
			if (dt.is(id))
			{
				if (!dt.isBound())
					dt.bind(this, tactic);
				return true;
			}
		return false;
	}
	
	@Override
	public String toString() {
		return which;
	}

	@Override
	public void print(PrettyPrinter pp, boolean withTactics) {
		pp.append("Strategem " + which);
		pp.requireNewline();
		if (withTactics) {
			pp.indentMore();
			pp.append("Tactics");
			pp.indentMore();
			for (Tactic t : strat.tactics())
			{
				pp.append(t);
				pp.requireNewline();
			}
			pp.indentLess();
			pp.append("Deferred");
			pp.indentMore();
			for (DeferredTactic dt : deferred)
			{
				dt.print(pp, false);
				pp.requireNewline();
			}
			pp.indentLess();
			pp.indentLess();
		}
	}

	public OrderedFileList sourceFiles() {
		if (strat == null)
			return new OrderedFileList();
		return strat.sourceFiles();
	}

	public Strategem getStrat() {
		return strat;
	}

	@Override
	public boolean isFirstTactic(Tactic tactic) {
		return strat.tactics().get(0) == tactic;
	}

	@Override
	public boolean isLastTactic(Tactic tactic) {
		return strat.tactics().get(strat.tactics().size()-1) == tactic;
	}

	public OrderedFileList ancillaryFiles() {
		if (strat instanceof HasAncillaryFiles)
			return ((HasAncillaryFiles)strat).getAncillaryFiles();
		return null;
	}

	@Override
	public Iterable<BuildResource> getDependencies(DependencyManager manager) {
		return manager.getDependencies(strat);
	}

	public boolean analyzeExports() {
		return strat.analyzeExports();
	}

	@Override
	public boolean isNotApplicable() {
		// TODO: one day they all will be 
		if (strat instanceof AbstractBuildCommand)
			return !((AbstractBuildCommand) strat).isApplicable();
		return false;
	}

	public void addGitTx(GitRecord tx) {
		gittxs.add(tx);
	}

	public void commitAll() {
		for (GitRecord gr : gittxs)
			gr.commit();
	}

	@Override
	public void fail() {
		for (GitRecord gr : gittxs)
			gr.setError();
	}
	
	
}