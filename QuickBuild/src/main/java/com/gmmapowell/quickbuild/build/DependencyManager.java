package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.graphs.Link;
import com.gmmapowell.graphs.Node;
import com.gmmapowell.graphs.NodeWalker;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;

/** This is contained within BuildContext
 * and is responsible for figuring out all the dependency stuff.
 *
 * Specific Responsibilities (for now at least):
 *   Ordering all the strategems and Tactics
 *   Loading & Saving dependencies and build order
 *   Offering an interface for "what should I build next?"
 *   Knowing if stuff needs building or not
 *   
 * All the organization should happen before we do anything
 * If we need to change in mid-flight, then that should be treated as such a special case, we go back to the beginning
 * We should also check all dirtiness here.
 * 
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */

// I feel that resource manager is probably a different guy, but 
// for now, it's better here than there.

// Build order (& strats) is probably also better as a relation
public class DependencyManager {
	public class ComparisonResource extends SolidResource {
		private final String comparison;
		private final String builtBy;
	
		public ComparisonResource(String from, String builtBy) {
			super(null, new File(FileUtils.getCurrentDir(), "unused"));
			this.comparison = from;
			this.builtBy = builtBy;
		}
	
		@Override
		public Strategem getBuiltBy() {
			if (builtBy == null)
				return null;
			if (stratMap == null)
				throw new UtilException("Cannot ask for strat before attaching them");
			else if (!stratMap.containsKey(builtBy))
				throw new UtilException("There is no strat matching " + builtBy);
			else
				return stratMap.get(builtBy);
		}
	
		@Override
		public File getPath() {
			throw new UtilException("Not implemented");
		}
	
		@Override
		public String compareAs() {
			return comparison;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (!(obj instanceof BuildResource))
				return false;
			return compareAs().equals(((BuildResource)obj).compareAs());
		}

		@Override
		public int hashCode() {
			return compareAs().hashCode();
		}
	}

	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final File dependencyFile;
	private final BuildOrder buildOrder;
	private final ResourceManager rm;
	private final boolean debug;
	private final HashMap<Strategem, Set<BuildResource>> cache = new HashMap<Strategem, Set<BuildResource>>();
	private HashMap<String, Strategem> stratMap = null;

	public DependencyManager(Config conf, ResourceManager rm, BuildOrder buildOrder, boolean debug)
	{
		this.rm = rm;
		this.buildOrder = buildOrder;
		this.debug = debug;
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
	}
	
	public void figureOutDependencies(List<Strategem> strats)
	{
		// Clear out any erroneous info from loading cache
		dependencies.clear();
		buildOrder.clear();
		
		// First off, build up a picture of what exists without prompting ...

		// OK, everything we've seen so far is built at the beginning of time ...
		Set<BuildResource> preBuilt = new HashSet<BuildResource>();
		for (BuildResource br : rm.current())
		{
			preBuilt.add(br);
			dependencies.ensure(br);
		}
		
		// Now, separately, let's look at what we could build, if we tried ...
		Set<BuildResource> willBuild = new HashSet<BuildResource>();
		Set<CloningResource> clones = new HashSet<CloningResource>();
		for (Strategem s : strats)
		{
			// Get it in the build order (at level 0)
			// buildOrder.depends(this, s, null);
			buildOrder.knowAbout(s);
			for (BuildResource br : s.buildsResources())
			{
				if (br instanceof CloningResource)
				{
					CloningResource cr = (CloningResource) br;
					BuildResource actual = cr.getActual();
					if (actual == null)
					{
						clones.add(cr);
						continue;
					}
					br = actual;
				}

				willBuild.add(br);
				dependencies.ensure(br);
			}
		}
		
		// Resolve any clones as best we can ...
		for (CloningResource clone : clones)
		{
			PendingResource pending = clone.getPending();
			BuildResource from = resolve(pending);
			BuildResource copy = from.cloneInto(clone);
			clone.bind(copy);
			dependencies.ensure(copy);
		}
		
		// Now wire up the guys that depend on it
		for (Strategem s : strats)
		{			
			s.buildsResources().resolveClones();

			for (PendingResource pr : s.needsResources())
			{
				if (pr == null)
					throw new QuickBuildException("The strategem " + s + " has a null 'needed' resource: " + s.needsResources());
				BuildResource actual = resolve(pr);
				dependencies.ensure(actual);
				for (BuildResource br : s.buildsResources())
				{
					if (br instanceof CloningResource)
					{
						CloningResource cr = (CloningResource) br;
						br = cr.getActual();
						if (br == null)
							throw new QuickBuildException("It's an error for a cloning resource to not be resolved by now");
					}
					
					dependencies.ensureLink(br, actual);
				}
			}
		}
		cache.clear();
	}

	BuildResource resolve(PendingResource pr) {
		if (pr.isBound())
			return pr.physicalResource();
		List<BuildResource> uniq = new ArrayList<BuildResource>();
		Pattern p = Pattern.compile(".*"+pr.compareAs().toLowerCase().replaceAll("\\.", "\\\\.")+".*");
		for (BuildResource br : dependencies.nodes())
		{
			if (br instanceof PendingResource || br instanceof ComparisonResource)
				continue;
			if (p.matcher(br.compareAs().toLowerCase()).matches())
			{
				uniq.add(br);
			}
		}
		if (uniq.size() == 0)
			throw new QuickBuildException("Could not find any dependency that matched " + pr.compareAs() +": have " + dependencies.nodes());
		else if (uniq.size() > 1)
			throw new QuickBuildException("Cannot resolve comparison: " + pr.compareAs() + " matches: " + uniq);
		pr.bindTo(uniq.get(0));
		return uniq.get(0);
	}

	public void loadDependencyCache()
	{
		if (!dependencyFile.canRead())
		{
			throw new QuickBuildCacheException("There was no dependency cache", null);
		}
		try
		{
			final XML input = XML.fromFile(dependencyFile);
			for (XMLElement e : input.top().elementChildren())
			{
				String from = e.get("from");
				String by = null;
				if (e.hasAttribute("builtBy"))
					by = e.get("builtBy");
				BuildResource target = new ComparisonResource(from, by);
				dependencies.ensure(target);
				for (XMLElement r : e.elementChildren())
				{
					String resource = r.get("resource");
					Node<BuildResource> source = dependencies.find(new ComparisonResource(resource, null));
//					System.out.println(target + " <= " + source);
					dependencies.ensureLink(target, source.getEntry());
				}
			}
			cache.clear();
		}
		catch (Exception ex)
		{
			// TODO: we should clear all the links out, but we need to keep the nodes
			dependencyFile.delete();
			throw new QuickBuildCacheException("Could not decipher the dependency cache", ex);
		}
	}

	public void saveDependencies() {
		final XML output = XML.create("1.0", "Dependencies");
		dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {
			@Override
			public void present(Node<BuildResource> node) {
				XMLElement dep = output.addElement("Dependency");
				BuildResource br = node.getEntry();
				dep.setAttribute("from", br.compareAs());
				if (br.getBuiltBy() != null)
					dep.setAttribute("builtBy", br.getBuiltBy().identifier());
				for (Link<BuildResource> l : node.linksFrom())
				{
					XMLElement ref = dep.addElement("References");
					BuildResource to = l.getTo();
					ref.setAttribute("resource", to.compareAs());
				}
			}

		});
		FileUtils.assertDirectory(dependencyFile.getParentFile());
		output.write(dependencyFile);
	}

	public Iterable<BuildResource> getDependencies(Strategem dependent) {
		if (cache.containsKey(dependent))
			return cache.get(dependent);
		
		Set<BuildResource> ret = new HashSet<BuildResource>();
		for (PendingResource pr : dependent.needsResources())
			ret.add(pr.physicalResource());
		for (BuildResource br : dependent.buildsResources())
		{
			findDependencies(ret, br);
		}
		cache.put(dependent, ret);
		return ret;
	}

	public Iterable<BuildResource> unBuilt()
	{
		List<BuildResource> ret = new ArrayList<BuildResource>();
		for (BuildResource br : dependencies.nodes())
			if (br.getBuiltBy() == null)
				ret.add(br);
		return ret;
	}
	
	public Iterable<Strategem> getAllStratsThatDependOn(Strategem buildFirst) {
		Set<Strategem> ret = new HashSet<Strategem>();
		for (BuildResource br : dependencies.nodes())
		{
			for (BuildResource wb : buildFirst.buildsResources())
				if (dependencies.hasLink(br, wb) && br.getBuiltBy() != null)
					ret.add(br.getBuiltBy());
		}
		return ret;
	}
	
	private void findDependencies(Set<BuildResource> ret, BuildResource br) {
		Iterable<BuildResource> allChildren = dependencies.allChildren(br);
		for (BuildResource a : allChildren)
		{
			ret.add(a);
			findDependencies(ret, a);
		}
	}

	public void clearCache() {
		dependencies.clear();
		cache.clear();
	}

	public String printableDependencyGraph() {
		return dependencies.toString();
	}

	public boolean addDependency(Strategem dependent, BuildResource resource, boolean wantDebug) {
		// The actual dependency is for the things to be built
		boolean ret = false;
		if (resource instanceof PendingResource)
			throw new QuickBuildException("No Way!");
		for (BuildResource br : dependent.buildsResources())
			ret |= addDependency(br, resource, wantDebug);
		return ret;
	}

	private boolean addDependency(BuildResource br, BuildResource resource, boolean wantDebug) {
		if (dependencies.hasLink(br, resource))
			return false;
		
		if (br instanceof PendingResource)
			throw new QuickBuildException("No Way!");
		if (resource instanceof PendingResource)
			throw new QuickBuildException("No Way!");
		if (debug || wantDebug)
			System.out.println("Added dependency from " + br + " on " + resource);
		dependencies.ensureLink(br, resource);
		buildOrder.reject(br.getBuiltBy());
		
		// TODO: this could be more subtle
		cache.clear();
		
		return true;
	}
	
	public void attachStrats(List<Strategem> strats) {
		stratMap = new HashMap<String, Strategem>();
		for (Strategem s : strats)
			stratMap.put(s.identifier(), s);
		try
		{
			// existing
			for (BuildResource br : rm.current())
			{
				Node<BuildResource> n = dependencies.find(br);
				if (n.getEntry() instanceof DependencyManager.ComparisonResource)
					n.setEntry(br);
			}
			// will be built
			Set<CloningResource> clones = new HashSet<CloningResource>();
			for (Strategem s : strats)
			{
				for (BuildResource br : s.buildsResources())
				{
					if (br instanceof CloningResource)
					{
						clones.add((CloningResource) br);
						continue;
					}
					Node<BuildResource> n = dependencies.find(br);
					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
						n.setEntry(br);
				}
			}
			
			// Resolve any clones as best we can ...
			for (CloningResource clone : clones)
			{
				PendingResource pending = clone.getPending();
				BuildResource from = resolve(pending);
				BuildResource actual = from.cloneInto(clone);
				clone.bind(actual);
				dependencies.ensure(actual);
			}

			// needed ones
			for (Strategem s : strats)
			{
				for (PendingResource pr : s.needsResources())
				{
					BuildResource br = resolve(pr);
					Node<BuildResource> n = dependencies.find(br);
					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
						n.setEntry(br);
				}
				for (Tactic tt : s.tactics())
					if (tt instanceof DependencyFloat)
					{
						ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
						for (PendingResource pr : addl)
							resolve(pr);
					}
			}
		}
		catch (Exception ex)
		{
			throw new QuickBuildCacheException("Failed to attach real strats to cache", ex);
		}
		cache.clear();
	}
}
