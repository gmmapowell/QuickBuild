package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public interface Strategem {
	Comparator<? super Strategem> Comparator = new Comparator<Strategem>() {
		@Override
		public int compare(Strategem o1, Strategem o2) {
			return o1.identifier().compareTo(o2.identifier());
		}
	};
	String identifier();
	File rootDirectory();
	List<? extends Tactic> tactics();
	boolean onCascade();
}
