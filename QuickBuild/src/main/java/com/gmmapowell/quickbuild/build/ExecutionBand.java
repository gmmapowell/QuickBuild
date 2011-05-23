package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gmmapowell.utils.PrettyPrinter;

public class ExecutionBand implements Iterable<BandElement> {
	private final int drift;
	private List<BandElement> elements = new ArrayList<BandElement>();

	public ExecutionBand(int drift) {
		this.drift = drift;
	}

	public void add(BandElement elt) {
		elements.add(elt);
	}

	public int drift() {
		return drift;
	}

	@Override
	public Iterator<BandElement> iterator() {
		return elements.iterator();
	}

	public int size() {
		return elements.size();
	}

	public BandElement get(int currentStrat) {
		return elements.get(currentStrat);
	}

	public void remove(BandElement be) {
		if (!elements.remove(be))
			throw new RuntimeException("Band did not contain " + be);
	}

	@Override
	public String toString() {
		return elements.toString();
	}

	public void print(PrettyPrinter pp) {
		for (BandElement be : elements)
		{
			be.print(pp);
		}
	}
}
