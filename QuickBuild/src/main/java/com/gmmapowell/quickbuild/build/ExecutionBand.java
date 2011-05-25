package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.PrettyPrinter;

public class ExecutionBand implements Iterable<BandElement> {
	private final int drift;
	private List<BandElement> elements = new ArrayList<BandElement>();

	public ExecutionBand(int drift) {
		this.drift = drift;
	}

	public void add(BandElement elt) {
		if (elt instanceof ExecuteStrategem)
		{
			((ExecuteStrategem)elt).bind(this);
			for (int i=elements.size()-1;i>=0;i--)
				if (elements.get(i) instanceof ExecuteStrategem)
				{
					elements.add(i+1, elt);
					return;
				}
			elements.add(0, elt);
			return;
		}
		else
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

	public void print(PrettyPrinter pp, boolean withTactics) {
		pp.append("Requires:");
		pp.indentMore();
		for (BandElement be : elements)
			be.showRequires(pp);
		pp.indentLess();
		for (BandElement be : elements)
		{
			be.print(pp, withTactics);
		}
	}
	
	public boolean hasPrereq(Strategem strat)
	{
		for (BandElement be : elements)
			if (be.hasPrereq(strat))
				return true;
		return false;
	}

	public Catchup getCatchup() {
		for (BandElement be : elements)
			if (be instanceof Catchup)
				return (Catchup)be;
		Catchup ret = new Catchup();
		elements.add(ret);
		return ret;
	}

	public boolean produces(BuildResource br) {
		for (BandElement be : elements)
			if (be instanceof ExecuteStrategem)
			{
				ExecuteStrategem es = (ExecuteStrategem)be;
				for (BuildResource r : es.getStrat().buildsResources())
					if (br.compareAs().equals(r.compareAs()))
						return true;
			}
		/*
			else if (be instanceof Catchup)
			{
				Catchup c = (Catchup)be;
				for (DeferredTactic dt : c.deferred)
					if (dt.getTactic().belongsTo().identifier().equals(br.getBuiltBy().identifier()))
						return true;
			}
			*/
		return false;
	}
}
