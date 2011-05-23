package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.Tactic;

/**
 * The purpose of this class is to communicate between BuildOrder (which knows what to
 * build) and BuildContext (which knows how to build), what should be built.
 *
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ItemToBuild {
	public final BuildStatus needsBuild;
	public final BandElement strat;
	public final Tactic tactic;
	public final String id;
	public final String label;
	
	public ItemToBuild(BuildStatus needsBuild, BandElement be, Tactic tactic, String id, String label)
	{
		this.needsBuild = needsBuild;
		this.strat = be;
		this.tactic = tactic;
		this.id = id;
		this.label = label;
	}
}
