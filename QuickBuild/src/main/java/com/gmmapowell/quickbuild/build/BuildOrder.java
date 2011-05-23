package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.FileUtils;
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
	private int targetFailures;

	public BuildOrder(Config conf, boolean buildAll)
	{
		this.buildAll = buildAll;
		buildOrderFile = new File(conf.getCacheDir(), "buildOrder.xml");
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
			es.bind(addToBand, toBuild);
		}
		// TODO Auto-generated method stub
		// TODO: the key is to use names, and then at the last minute tie the names to the actual objects ...
		toBuild.identifier();
	}
	
	private ExecutionBand require(int toBand, int drift) {
		if (toBand < bands.size())
			if (bands.get(toBand).drift() == drift)
				return bands.get(toBand);
		// More drift stuff here?
		ExecutionBand ret = new ExecutionBand(drift);
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


	/*
	ExecuteStrategem tryToFloatDownwards() {
		// So, we've found that this one would like to float down.
		// It can't go below anyone who wants to go down more, or
		// anyone that it's dependent on (transitively), but it should be able to move
		// them down too.
		// I'm leaving that later case for when it arises.
		
		Strategem me = currentStrat.getBuiltBy();
		int pri = ((FloatToEnd)me).priority();
		int curpos;
		for (curpos = strategemToExecute+1;curpos < strats.size();curpos++)
		{
			if (dependencies.hasLink(strats.get(curpos), currentStrat))
				break;
			Strategem compareTo = strats.get(curpos).getBuiltBy();
			if (!(compareTo instanceof FloatToEnd))
				continue;
			if (pri <= ((FloatToEnd)compareTo).priority())
				break;
		}
		if (curpos != strategemToExecute+1)
		{
			strats.add(curpos, currentStrat);
			strats.remove(strategemToExecute);
		}
		return strats.get(strategemToExecute);
	}
*/
	
	enum Status { BEGIN, BUILD_THIS, MOVE_ON, RETRY };
	Status status = Status.BEGIN;

	private int currentBand;
	private int currentStrat;
	private int currentTactic;

	public ItemToBuild next() {
		for (;;)
		{
			if (status == Status.BEGIN)
			{
				currentBand = 0;
				currentStrat = 0;
				currentTactic = -1;
				status = Status.BUILD_THIS;
			}
			if (currentBand >= bands.size())
				return null;
			ExecutionBand band = bands.get(currentBand);
			if (status == Status.RETRY)
			{
				currentStrat = 0;
				currentTactic = 0;
			}
			if (currentStrat >= band.size())
			{
				currentBand++;
				currentStrat = 0;
				currentTactic = -1;
				continue;
			}
			BandElement be = band.get(currentStrat);
			if (status == Status.MOVE_ON || currentTactic == -1)
				currentTactic++;
			if (currentTactic >= be.size())
			{
				currentStrat++;
				if (currentStrat < band.size())
					System.out.println("Advancing to " + band.get(currentStrat));
				currentTactic = -1;
				continue;
			}
			BuildStatus bs = BuildStatus.SUCCESS;
			Tactic tactic = be.tactic(currentTactic);
			if (be.isDeferred(tactic))
			{
				bs = BuildStatus.DEFERRED;
			}
			return new ItemToBuild(bs, be, tactic, currentBand + "." + currentStrat+"."+currentTactic, tactic.toString());
		}
	}

	public void advance() {
		status = Status.MOVE_ON;
	}


	public void tryAgain() {
		// I just think this is a basically broken test ...
		/*
		if (++targetFailures >= 10)
			throw new UtilException("The strategy " + currentStrat + " failed 5 times in a row");
			*/
		status = Status.RETRY;
	}

	public void markDirty(Strategem d) {
		// TODO Auto-generated method stub
		
	}

	public void forceRebuild() {
//		manager.getGitCacheFile(stratFor(bc)).delete();
		
	}

	public static String tacticIdentifier(Strategem parent, String suffix) {
		String ret = parent.identifier();
		if (!ret.endsWith("]"))
			throw new RuntimeException("Identifiers should end with ]: " + ret);
		return ret.substring(0, ret.length()-1) + suffix + "]";
	}

}
