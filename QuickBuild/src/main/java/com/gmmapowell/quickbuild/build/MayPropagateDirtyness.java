package com.gmmapowell.quickbuild.build;

public interface MayPropagateDirtyness {
	/** Returns true if the dirtyness was sufficient
	 * that people who differentiate between "external" and "internal" dirtyness
	 * should rebuild anyway.
	 * 
	 * Those who don't care shouldn't call this method in the first place.
	 * 
	 * If this returns false, and you don't care, don't rebuild
	 * 
	 * @return true if the dirtyness propagates (eg an API change)
	 */
	public boolean dirtynessPropagates();
}
