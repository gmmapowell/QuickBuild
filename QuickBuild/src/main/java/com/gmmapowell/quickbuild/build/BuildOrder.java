package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.git.GitHelper;
import com.gmmapowell.git.GitRecord;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
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
	private final List<BandElement> pending = new ArrayList<BandElement>();
	
	// This is the basic hierarchy ... everything in the same "band" is independent, but dependent on _something_ in the previous band
	// We can build the members of a band in any order
	// We can build an entire band before aborting
	private final List<ExecutionBand> bands = new ArrayList<ExecutionBand>();
	private final File buildOrderFile;
	private boolean buildAll;
	private final boolean debug;

	private final BuildContext cxt;
	private DependencyManager dependencies;
	private final Set<BuildResource> dirtyUnbuilt = new HashSet<BuildResource>();
	private final Set<GitRecord> ubtxs = new HashSet<GitRecord>();

	public BuildOrder(BuildContext cxt, boolean buildAll, boolean debug)
	{
		this.cxt = cxt;
		this.buildAll = buildAll;
		this.debug = debug;
		buildOrderFile = cxt.getCacheFile("buildOrder.xml");
	}	

	public void dependencyManager(DependencyManager manager) {
		this.dependencies = manager;
	}

	public void clear() {
		mapping.clear();
		bands.clear();
	}
	
	public void buildAll() {
		buildAll = true;
	}

	public void knowAbout(Strategem s) {
		mapping.put(s.identifier(), new ExecuteStrategem(s.identifier()));
		ExecuteStrategem es = mapping.get(s.identifier());
		es.bind(s);
		pending.add(es);
	}

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
							es.defer(dt);
							deferred.add(dt);
						}
					}
					else if (strat.tag().equals("Deferred"))
					{
						String name = strat.get("name");
						for (DeferredTactic dt : deferred)
							if (dt.is(name))
							{
								deferred.remove(dt);
								band.add(dt);
								dt.bind(band);
								break;
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
				if (be instanceof ExecuteStrategem)
				{
					ExecuteStrategem es = (ExecuteStrategem)be;
					esi = item.addElement("Strategem");
					esi.setAttribute("name", es.name());
					for (DeferredTactic dt : be.deferred())
					{
						XMLElement def = esi.addElement("Defer");
						def.setAttribute("name", dt.name());
					}
				}
				else if (be instanceof DeferredTactic)
				{
					DeferredTactic dt = (DeferredTactic) be;
					esi = item.addElement("Deferred");
					esi.setAttribute("name", dt.name());
				}
				else
					throw new RuntimeException("Cannot handle " + be);
			}
		}
		FileUtils.assertDirectory(buildOrderFile.getParentFile());
		output.write(buildOrderFile);
	}

	public static String tacticIdentifier(Strategem parent, String suffix) {
		String ret = parent.identifier();
		if (!ret.endsWith("]"))
			throw new RuntimeException("Identifiers should end with ]: " + ret);
		return ret.substring(0, ret.length()-1) + "-" + suffix + "]";
	}

	public String printOut(boolean withTactics) {
		PrettyPrinter pp = new PrettyPrinter();
		pp.indentWidth(2);
		pp.indentMore();
		int i=0;
		for (ExecutionBand b : bands)
		{
			pp.append("Band " + i);
			if (b.drift() > 0)
				pp.append(" (drift " + b.drift() +")");
			pp.indentMore();
			b.print(pp, withTactics);
			pp.indentLess();
			i++;
		}
		return pp.toString();
	}

	public void figureDirtyness(DependencyManager manager) {
		for (BuildResource br : manager.unBuilt())
		{
			File f = br.getPath();
			OrderedFileList ofl = new OrderedFileList(f);
			GitRecord ubtx = GitHelper.checkFiles(!buildAll, ofl, cxt.getGitCacheFile("Unbuilt_"+f.getName(), ""));
			ubtxs.add(ubtx);
			if (ubtx.isDirty())
				dirtyUnbuilt.add(br);
		}
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
		for (BandElement es : pending)
		{
			if (es instanceof ExecuteStrategem)
				figureDirtyness(manager, (ExecuteStrategem) es);
		}
	}

	public void figureDirtyness(DependencyManager manager, ExecuteStrategem strat) {
		if (strat == null || strat.getStrat() == null)
			return;
		OrderedFileList files = strat.sourceFiles();
		boolean isDirty = false;
		if (buildAll)
		{
			if (debug)
				System.out.println("Marking " + strat + " dirty due to --build-all");
			isDirty = true;
		}
		boolean wasDirty = isDirty;
		if (files == null)
		{
			isDirty = true;
			if (!wasDirty && debug)
				System.out.println("Marking " + strat + " dirty due to NULL file list");
		}
		else
		{
			GitRecord gittx = GitHelper.checkFiles(strat.isClean() && !buildAll, files, cxt.getGitCacheFile(strat.name(), ""));
			strat.addGitTx(gittx);
			isDirty |= gittx.isDirty();
			if (!wasDirty && isDirty && debug)
				System.out.println("Marking " + strat + " dirty due to git hash-object");
		}
		if (!isDirty)
		{
			for (BuildResource wb : strat.getStrat().buildsResources())
			{
				if (wb.getPath() == null || !wb.getPath().exists())
				{
					if (debug)
						System.out.println("Marking " + strat + " dirty because " + wb.compareAs() + " does not have a file output");
					isDirty = true;
				}
				else if (!wb.getPath().exists())
				{
					if (debug)
						System.out.println("Marking " + strat + " dirty because " + wb.compareAs() + " does not exist");
					isDirty = true;
				}
			}
		}
		if (!isDirty)
		{
			for (BuildResource d : manager.getDependencies(strat.getStrat()))
			{
				if (d.getBuiltBy() == null)
				{
					if (dirtyUnbuilt.contains(d))
					{
						isDirty = true;
						if (debug)
							System.out.println("Marking " + strat + " dirty due to library " + d + " is dirty");
					}
				}
				else if (!mapping.get(d.getBuiltBy().identifier()).isClean())
				{
					isDirty = true;
					if (debug)
						System.out.println("Marking " + strat + " dirty due to " + d + " is dirty");
				}
			}
		}
		OrderedFileList ancillaries = strat.ancillaryFiles();
		boolean ancDirty = false;
		if (ancillaries != null && !ancillaries.isEmpty())
		{
			 GitRecord ancTx = GitHelper.checkFiles(strat.isClean() && !buildAll, ancillaries, cxt.getGitCacheFile(strat.name(), ".anc"));
			 strat.addGitTx(ancTx);
			 ancDirty = ancTx.isDirty();
		}

		if (isDirty || buildAll)
		{
			strat.markDirty();
		}
		else if (ancDirty) {
			if (debug)
				System.out.println("Marking " + strat + " locally dirty due to git hash-object on ancillaries");
			strat.markDirtyLocally();
		}
	}

	public ItemToBuild get(int band, int strat, int tactic) {
		if (band >= bands.size())
		{
			if (!addFromWell())
				return null;
		}
		ExecutionBand exband = bands.get(band);
		if (strat >= exband.size())
		{
			if (!addFromWell() || strat >= exband.size())
				return null;
		}
		BandElement be = exband.get(strat);
		if (tactic >= be.size())
			return null;
		Tactic tt = be.tactic(tactic);
		
		BuildStatus bs = BuildStatus.SUCCESS;
		if (be.isNotApplicable())
			bs = BuildStatus.NOTAPPLICABLE;
		else if (be.isDeferred(tt))
			bs = BuildStatus.SKIPPED;
		else if (be instanceof DeferredTactic) {
			bs = BuildStatus.DEFERRED;
			if (((DeferredTactic)be).isCompletelyClean())
				bs = BuildStatus.CLEAN;
		}
		if (be.isCompletelyClean())
			bs = BuildStatus.CLEAN;
		return new ItemToBuild(bs, be, tt, (band+1) + "." + (strat+1)+"."+(tactic+1), tt.toString());
	}

	private boolean addFromWell() {
		if (pending.size() == 0)
			return false;
		BandElement canOffer = null;
		int withDrift = -2;
		int offerAt = -2;
		boolean willSplit = false;
		loop:
		for (BandElement p : pending)
		{
			if (debug)
			{
				System.out.println("Considering " + p.name());
			}
			int drift = getDrift(p);
			if (withDrift != -2 && drift > withDrift)
				continue;
			if (p instanceof DeferredTactic)
			{
				BuildResource reject = null;
				for (BuildResource sr : p.getStrat().buildsResources())
					if (isBuilt(sr) < 0)
						reject = sr;
				if (reject != null)
				{
					if (debug)
						System.out.println("  Rejecting " + p.name() + "because its parent has not built");
					continue;
				}
			}
			int maxBuilt = -1;
			boolean hasDependency = false;
			for (BuildResource pr : p.getDependencies(dependencies))
			{
				int builtAt = isBuilt(pr);
				if (builtAt == -2)
				{
					if (debug)
						System.out.println("  Rejecting because " + pr + " is not built");
					hasDependency = true;
					continue;
				}
				else if (hasDependency)
					continue;
				if (debug)
					System.out.println("  Dependency " + pr + " was built at " + builtAt);
				maxBuilt = Math.max(maxBuilt, builtAt);
			}
			if (hasDependency)
				continue loop;
			boolean needsSplit = needSplit(p);
			if (offerAt == -2 || drift < withDrift || (drift == withDrift && maxBuilt < offerAt) /*|| (drift == withDrift && maxBuilt == offerAt && (willSplit && !needsSplit))*/)
			{
				willSplit = needsSplit;
				offerAt = maxBuilt;
				withDrift = drift;
				canOffer = p;
				if (debug)
				{
					System.out.println("  Will consider " + p.name() + " drift = " + drift + " at " + maxBuilt + " split = " + needsSplit);
					if (p.name().equals("Deploy[tomcat]"))
						System.out.println("Why?");
				}
			}
		}
		// TODO: at this point, we should come back and see if there is a dependency which has
		// a higher drift value that its dependent (shouldn't happen though).
		if (canOffer != null)
		{
			if (debug)
				System.out.println("And trying " + canOffer.name() + " drift = " + withDrift + " at " + offerAt + " split = " + willSplit);
			if (willSplit)
				splitFloaters((ExecuteStrategem)canOffer);
			pending.remove(canOffer);
			addTo(offerAt+1, canOffer);
			if (debug)
				System.out.println(printOut(false));
			return true;
		}
		System.out.println("There is nothing in the well that can be added!");
		System.out.println("Well contents:");
		for (BandElement p : pending)
		{
			System.out.println("  Band " + p.name() + " drift: " + getDrift(p));
			if (p instanceof DeferredTactic)
			{
				BuildResource reject = null;
				for (BuildResource sr : p.getStrat().buildsResources())
					if (isBuilt(sr) < 0)
						reject = sr;
				if (reject != null)
				{
					System.out.println("    Rejecting " + p.name() + "because its parent has not built");
					continue;
				}
			}
			int maxBuilt = -1;
			boolean hasDependency = false;
			for (BuildResource pr : p.getDependencies(dependencies))
			{
				int builtAt = isBuilt(pr);
				if (builtAt == -2)
				{
					System.out.println("    Rejecting because " + pr + " is not built");
					hasDependency = true;
					continue;
				}
				else if (hasDependency)
					continue;
				maxBuilt = Math.max(maxBuilt, builtAt);
			}
		}
		throw new UtilException("There is no way to build everything");
	}

	private void splitFloaters(ExecuteStrategem es) {
		for (Tactic tt : es.getStrat().tactics())
		{
			if (es.isDeferred(tt))
				continue;
			if (!(tt instanceof DependencyFloat))
				continue;
			ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
			boolean needSplit = false;
			for (PendingResource pr : addl)
			{
				BuildResource br = dependencies.resolve(pr);
				if (isBuilt(br) == -2)
					needSplit = true;
			}
			if (needSplit)
			{
				DeferredTactic dt = new DeferredTactic(tt.identifier());
				dt.bind(es, tt);
				es.defer(dt);
				// add it to start of pending to try and keep things together
				pending.add(0, dt);
			}
		}
	}

	private boolean needSplit(BandElement p) {
		if (p instanceof DeferredTactic)
			return false;
		boolean needSplit = false;
		for (Tactic tt : p.getStrat().tactics())
		{
			if (p.isDeferred(tt))
				continue;
			if (tt instanceof DependencyFloat)
			{
				ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
				for (PendingResource pr : addl)
				{
					BuildResource br = dependencies.resolve(pr);
					if (isBuilt(br) == -2)
						needSplit = true;
				}
			}
		}
		return needSplit;
	}

	private int getDrift(BandElement be) {
		Strategem building = be.getStrat();
		if (building instanceof FloatToEnd)
			return ((FloatToEnd)building).priority();
		return 0;
	}

	private int isBuilt(BuildResource pr) {
		if (pr instanceof PendingResource && !((PendingResource) pr).isBound())
			return -2;
		Strategem s = pr.getBuiltBy();
		if (s == null)
			return -1;
		ExecuteStrategem es = mapping.get(s.identifier());
		for (int i=0;i<bands.size();i++)
			if (bands.get(i).contains(es))
				return i;
		return -2;
	}

	private void addTo(int band, BandElement canOffer) {
		int drift = getDrift(canOffer);
		while (band<bands.size())
		{
			if (bands.get(band).drift() == drift)
				break;
			else if (bands.get(band).drift() > drift)
				throw new UtilException("This shouldn't happen - bands have been added in wrong drift order");
			band++;
		}
		if (band >= bands.size())
			makeNew(canOffer.getStrat(), band, drift);
		bands.get(band).add(canOffer);
		if (canOffer instanceof ExecuteStrategem)
			((ExecuteStrategem)canOffer).markDirty();
	}

	private ExecutionBand makeNew(Strategem building, int toBand, int drift) {
		ExecutionBand ret = new ExecutionBand(drift);
		while (toBand < bands.size() && bands.get(toBand).drift() < drift)
		{
			toBand++;
		}
		bands.add(toBand, ret);
		return ret;
	}

	public ExecuteStrategem get(int band, int strat) {
		ExecutionBand exband = bands.get(band);
		BandElement be = exband.get(strat);
		if (be instanceof ExecuteStrategem)
			return (ExecuteStrategem) be;
		return null;
	}

	public void reject(Strategem s) {
		if (!mapping.containsKey(s.identifier()))
			throw new UtilException("Cannot reject non-existent " + s.identifier() + " have " + mapping.keySet());
		ExecuteStrategem es = mapping.get(s.identifier());
		for (ExecutionBand b : bands)
			if (b.contains(es))
				b.remove(es);
		if (!pending.contains(es))
		{
			System.out.println("Rejecting strategem " + s);
			pending.add(es);
		
			// and then make sure all dependents are rejected
			for (Strategem d : dependencies.getAllStratsThatDependOn(s))
				reject(d);
		}
	}

	public int count(int band) {
		return bands.get(band).size();
	}

	public void commitUnbuilt()
	{
		for (GitRecord gr : ubtxs)
			gr.commit();
	}

	public void commitAll()
	{
		for (ExecutionBand eb : this.bands)
			for (BandElement es : eb)
				if (es instanceof ExecuteStrategem)
					((ExecuteStrategem)es).commitAll();
	}
}
