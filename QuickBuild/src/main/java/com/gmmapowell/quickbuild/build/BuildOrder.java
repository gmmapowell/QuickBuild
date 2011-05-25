package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
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
public class BuildOrder {
	// We are going to track everything based on name, so we need to map name to strategem
	private final Map<String, ExecuteStrategem> mapping = new HashMap<String, ExecuteStrategem>();
	
	// This is the basic hierarchy ... everything in the same "band" is independent, but dependent on _something_ in the previous band
	// We can build the members of a band in any order
	// We can build an entire band before aborting
	private final List<ExecutionBand> bands = new ArrayList<ExecutionBand>();
	private final File buildOrderFile;
	private boolean buildAll;

	private final BuildContext cxt;

	public BuildOrder(BuildContext cxt, boolean buildAll)
	{
		this.cxt = cxt;
		this.buildAll = buildAll;
		buildOrderFile = cxt.getCacheFile("buildOrder.xml");
	}	

	public void clear() {
		mapping.clear();
		bands.clear();
	}
	
	public void buildAll() {
		buildAll = true;
	}

	public void depends(Strategem toBuild, Strategem mustHaveBuilt) {
		ExecuteStrategem es;
		String name = toBuild.identifier();
		int inBand = -1;
		int toBand = 0;
		int drift = 0; // TODO: figure out drift for e.g. javadoc
		if (toBuild instanceof FloatToEnd)
			drift = ((FloatToEnd)toBuild).priority();
		if (!mapping.containsKey(name))
		{
			es = new ExecuteStrategem(name);
			mapping.put(name, es);
		}
		else
		{
			es = mapping.get(name);
			toBand = inBand = findBandPos(es.inBand());
		}
		if (mustHaveBuilt != null)
		{
			if (!mapping.containsKey(mustHaveBuilt.identifier()))
				throw new RuntimeException("There is no previously built " + mustHaveBuilt.identifier());
			ExecuteStrategem prev = mapping.get(mustHaveBuilt.identifier());
			int prevBand = findBandPos(prev.inBand());
			if (inBand <= prevBand)
				toBand = prevBand+1;
		}
		if (toBand != inBand)
		{
			if (inBand != -1)
				bands.get(inBand).remove(es);
			ExecutionBand addToBand = require(toBand, drift);
			addToBand.add(es);
			es.bind(addToBand);
			es.bind(toBuild);
		}
	}
	
	private ExecutionBand require(int toBand, int drift) {
		if (toBand < bands.size())
			if (bands.get(toBand).drift() == drift)
				return bands.get(toBand);
		ExecutionBand ret = new ExecutionBand(drift);
		// Is it possible there are things that depend on this that are now going
		// to be de-prioritized?
		while (toBand < bands.size() && bands.get(toBand).drift() < drift)
			toBand++;
		bands.add(toBand, ret);
		return ret;
	}

	private int findBandPos(ExecutionBand inBand) {
		for (int i=0;i<bands.size();i++)
			if (bands.get(i) == inBand)
				return i;
		throw new RuntimeException("There is no band " + inBand);
	}

	/* Sample File:

	<?xml version="1.0" encoding="ISO-8859-1"?>
	<BuildOrder>
		<Band>
	    	<Strategem name="Jar[API.jar]"/>
	    	<Strategem name="Jar[Kernel.jar]"/>
	    	<Strategem name="Jar[Platform.jar]">
	    		<Defer name="JUnit[Platform.jar-src]"/>
	    	</Strategem>
	    </Band>
		<Band>
	    	<Strategem name="Jar[Cluster.jar]"/>
	    	<Strategem name="Jar[MockKernel.jar]"/>
	    	<Catchup>
	    		<Deferred name="JUnit[Platform.jar-src]"/>
	    	</Catchup>
	    </Band>
		<Band>
	    	<Strategem name="War[ziniki.war]"/>
	    </Band>
	    <Band drift="5">
	    	<Strategem name="JavaDoc[]"/>
	    </Band>
	</BuildOrder>
		 */
	// TODO: this needs to change to assume that it is called blank
	void loadBuildOrderCache() {
		if (!buildOrderFile.canRead())
		{
			throw new QuickBuildCacheException("There was no build order cache", null);
		}
		try
		{
			final XML input = XML.fromFile(buildOrderFile);
			List<DeferredTactic> deferred = new ArrayList<DeferredTactic>();
			for (XMLElement bandElt : input.top().elementChildren())
			{
				int drift = 0;
				if (bandElt.hasAttribute("drift"))
					drift  = Integer.parseInt(bandElt.get("drift"));
				ExecutionBand band = new ExecutionBand(drift);
				bands.add(band);
	
				for (XMLElement strat : bandElt.elementChildren())
				{
					if (strat.tag().equals("Strategem"))
					{
						String name = strat.get("name");
						ExecuteStrategem es = new ExecuteStrategem(name);
						band.add(es);
						es.bind(band);
						for (XMLElement defer : strat.elementChildren())
						{
							DeferredTactic dt = new DeferredTactic(defer.get("name"));
							es.add(dt);
							deferred.add(dt);
						}
					}
					else if (strat.tag().equals("Catchup"))
					{
						Catchup c = new Catchup();
						band.add(c);
						c.bind(band);
						for (XMLElement defer : strat.elementChildren())
						{
							String name = defer.get("name");
							for (DeferredTactic dt : deferred)
								if (dt.is(name))
								{
									c.add(dt);
									deferred.remove(dt);
									break;
								}
						}
					}
					else
						throw new RuntimeException("The tag " + strat.tag() + " was not valid");
				}
			}
			if (deferred.size() > 0)
				throw new RuntimeException("There were un-caught-up deferred tactics in the cache file");
		} catch (Exception ex)
		{
			buildOrderFile.delete();
			throw new QuickBuildCacheException("Could not parse build order cache", ex);
		}
	}

	public void attachStrats(List<Strategem> strats) {
		loop:
		for (Strategem s : strats)
		{
			for (ExecutionBand b : bands)
			{
				for (BandElement be : b)
				{
					if (be instanceof ExecuteStrategem && be.is(s.identifier()))
					{
						ExecuteStrategem es = (ExecuteStrategem)be;
						mapping.put(s.identifier(), es);
						es.bind(b);
						es.bind(s);
						continue loop;
					}
				}
			}
			throw new QuickBuildCacheException("There was no mapping for " + s.identifier(), null);
		}
	}

	public void saveBuildOrder() {
		final XML output = XML.create("1.0", "BuildOrder");
		for (ExecutionBand band : bands)
		{
			XMLElement item = output.addElement("Band");
			if (band.drift() != 0)
				item.setAttribute("drift", "" + band.drift());
			for (BandElement be : band)
			{
				XMLElement esi;
				String tag;
				if (be instanceof ExecuteStrategem)
				{
					ExecuteStrategem es = (ExecuteStrategem)be;
					esi = item.addElement("Strategem");
					esi.setAttribute("name", es.name());
					tag = "Defer";
				}
				else if (be instanceof Catchup)
				{
//					Catchup c = (Catchup)be;
					esi = item.addElement("Catchup");
					tag = "Deferred";
				}
				else
					throw new RuntimeException("Cannot handle " + be);
				for (DeferredTactic dt : be.deferred())
				{
					XMLElement def = esi.addElement(tag);
					def.setAttribute("name", dt.name());
				}
			}
		}
		FileUtils.assertDirectory(buildOrderFile.getParentFile());
		output.write(buildOrderFile);
	}

	public static String tacticIdentifier(Strategem parent, String suffix) {
		String ret = parent.identifier();
		if (!ret.endsWith("]"))
			throw new RuntimeException("Identifiers should end with ]: " + ret);
		return ret.substring(0, ret.length()-1) + suffix + "]";
	}

	public String printOut() {
		PrettyPrinter pp = new PrettyPrinter();
		int i=0;
		for (ExecutionBand b : bands)
		{
			pp.append("Band " + i);
			pp.indentMore();
			b.print(pp);
			pp.indentLess();
			i++;
		}
		return pp.toString();
	}

	public void figureDirtyness(DependencyManager manager) {
		for (ExecutionBand b : bands)
		{
			for (BandElement be : b)
			{
				if (be instanceof ExecuteStrategem)
				{
					ExecuteStrategem es = (ExecuteStrategem)be;
					figureDirtyness(manager, es);
				}
			}
		}
	}

	public void figureDirtyness(DependencyManager manager, ExecuteStrategem strat) {
		OrderedFileList files = strat.sourceFiles();
		boolean isDirty;
		if (buildAll)
			System.out.println("Marking " + strat + " dirty due to --build-all");
		if (files == null)
		{
			isDirty = true;
			System.out.println("Marking " + strat + " dirty due to NULL file list");
		}
		else
		{
			isDirty = GitHelper.checkFiles(strat.isClean() && !buildAll, files, cxt.getGitCacheFile(strat, ""));
			if (isDirty)
				System.out.println("Marking " + strat + " dirty due to git hash-object");
		}
		if (!isDirty)
		{
			for (BuildResource wb : strat.getStrat().buildsResources())
				if (!wb.getPath().exists())
				{
					System.out.println("Marking " + strat + " dirty because " + wb.compareAs() + " does not exist");
					isDirty = true;
				}
		}
		if (!isDirty)
		{
			for (BuildResource d : manager.getDependencies(strat.getStrat()))
			{
				if (d.getBuiltBy() == null)
					continue;
				if (!mapping.get(d.getBuiltBy().identifier()).isClean())
				{
					isDirty = true;
					System.out.println("Marking " + strat + " dirty due to " + d + " is dirty");
				}
			}
		}
		if (isDirty || buildAll)
		{
			strat.markDirty();
		}
		else
		{
			OrderedFileList ancillaries = strat.ancillaryFiles();
			if (ancillaries != null)
			{
				isDirty = GitHelper.checkFiles(strat.isClean() && !buildAll, ancillaries, cxt.getGitCacheFile(strat, ".anc"));
				if (isDirty)
				{
					System.out.println("Marking " + strat + " dirty due to git hash-object");
					strat.markDirtyLocally();
				}
			}
		}
	}

	public ItemToBuild get(int band, int strat, int tactic) {
		if (band >= bands.size())
			return null;
		ExecutionBand exband = bands.get(band);
		if (strat >= exband.size())
			return null;
		BandElement be = exband.get(strat);
		if (tactic >= be.size())
			return null;
		Tactic tt = be.tactic(tactic);
		
		BuildStatus bs = BuildStatus.SUCCESS;
		if (be.isDeferred(tt))
		{
			bs = BuildStatus.DEFERRED;
		}
		else if (be.isCompletelyClean())
			bs = BuildStatus.CLEAN;
		return new ItemToBuild(bs, be, tt, (band+1) + "." + (strat+1)+"."+(tactic+1), tt.toString());
	}

	public ExecuteStrategem get(int band, int strat) {
		ExecutionBand exband = bands.get(band);
		return (ExecuteStrategem) exband.get(strat);
	}
}
