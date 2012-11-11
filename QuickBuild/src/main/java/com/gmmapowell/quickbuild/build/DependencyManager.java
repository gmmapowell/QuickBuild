package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.gmmapowell.quickbuild.core.ProcessResource;
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
		public Tactic getBuiltBy() {
			if (builtBy == null)
				return null;
			if (stratMap == null)
				throw new UtilException("Cannot ask for strat before attaching them");
			else if (!stratMap.containsKey(builtBy))
				throw new UtilException("There is no strat matching " + builtBy);
			else
			{
				throw new UtilException("MISSING CODE DM1");
				// return stratMap.get(builtBy);
			}
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
	private final Map<Tactic, Set<BuildResource>> cache = new HashMap<Tactic, Set<BuildResource>>();
	private HashMap<String, Strategem> stratMap = null;
	private final Map<String, ProcessResource> processResources = new HashMap<String, ProcessResource>();

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
				
				// TODO: we should be iterating over Tactics, that should be the things responsible for saying what dependencies are ...
				// But for now, put it on the last guy
				List<? extends Tactic> tactics = s.tactics();
				Tactic last = tactics.get(tactics.size()-1);
				BuildResource proc = ensureProcessResource(last);
				dependencies.ensureLink(br, proc);
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
			for (Tactic tt : s.tactics())
				if (tt instanceof DependencyFloat)
				{
					ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
					for (PendingResource pr : addl)
						resolve(pr);
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
		if (uniq.size() > 1) {
			int i = 0;
			while (i<uniq.size()) {
				BuildResource me = uniq.get(i);
				if (me instanceof ProcessResource)
					uniq.remove(i);
				else
					i++;
			}
			if (uniq.size() == 0)
				throw new QuickBuildException("Found multiple derived dependencies that matched " + pr.compareAs() +", but no originals");
		}
		if (uniq.size() > 1)
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
				BuildResource target;
				if (e.tag().equals("Resource")) {
					target = new ComparisonResource(e.get("name"), null);
				} else if (e.tag().equals("Build")) {
					target = new ComparisonResource(e.get("builds"), e.get("tactic"));
				} else if (e.tag().equals("Process")) {
					target = new ComparisonResource("Process["+ e.get("tactic") + "]", null);
				}
				else
					throw new UtilException("Unrecognized dependency node: " + e.tag());
				dependencies.ensure(target);
				for (XMLElement r : e.elementChildren())
				{
					if (r.tag().equals("DependsOn")) {
						String resource = r.get("resource");
						Node<BuildResource> source = dependencies.find(new ComparisonResource(resource, null));
//						System.out.println(target + " <= " + source);
						dependencies.ensureLink(target, source.getEntry());
					} else
						throw new UtilException("Cannot handle inner tag: " + r.tag());
				}
			}
			cache.clear();
		}
		catch (Exception ex)
		{
			// TODO: we should clear all the links out, but we need to keep the nodes
			buildOrder.clear();
//			dependencyFile.delete();
//			throw new QuickBuildCacheException("Could not decipher the dependency cache", ex);
			throw new UtilException("Could not decipher the dependency cache", ex);
		}
	}

	public void saveDependencies() {
		final XML output = XML.create("1.0", "Dependencies");
		dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {
			@Override
			public void present(Node<BuildResource> node) {
				BuildResource br = node.getEntry();
				XMLElement dep;
				if (br instanceof ProcessResource) {
					dep = output.addElement("Process");
					dep.setAttribute("tactic", ((ProcessResource)br).getTactic().identifier());
				}
				else if (br.getBuiltBy() == null) {
					XMLElement res = output.addElement("Resource");
					res.setAttribute("name", br.compareAs());
					return;
				} else {
					dep = output.addElement("Build");
					dep.setAttribute("builds", br.compareAs());
					dep.setAttribute("tactic", br.getBuiltBy().identifier());
				}
				for (Link<BuildResource> l : node.linksFrom())
				{
					XMLElement ref = dep.addElement("DependsOn");
					BuildResource to = l.getTo();
					ref.setAttribute("resource", to.compareAs());
				}
			}

		});
		FileUtils.assertDirectory(dependencyFile.getParentFile());
		output.write(dependencyFile);
	}

	public Iterable<BuildResource> getDependencies(Tactic tactic) {
//		if (cache.containsKey(tactic))
//			return cache.get(tactic);
		
		Set<BuildResource> ret = new HashSet<BuildResource>();
		for (PendingResource pr : tactic.belongsTo().needsResources()) {
			BuildResource br = pr.physicalResource();
			if (ret.add(br))
				findDependencies(ret, br);
		}
//		for (BuildResource br : tactic.belongsTo().buildsResources())
//		{
//			findDependencies(ret, br);
//		}
		BuildResource dep = ensureProcessResource(tactic);
		for (Tactic t : tactic.getProcessDependencies()) {
			if (t == null) continue;
			BuildResource to = ensureProcessResource(t);
			dependencies.ensureLink(dep, to);
		}
		findDependencies(ret, dep);
//		cache.put(tactic, ret);
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
	
	private void findDependencies(Set<BuildResource> ret, BuildResource br) {
		Iterable<BuildResource> allChildren = dependencies.allChildren(br);
		for (BuildResource a : allChildren)
		{
			if (ret.contains(a))
				continue;
			if (ret.add(a))
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

	public boolean addDependency(Tactic dependent, BuildResource resource, boolean wantDebug) {
		if (resource instanceof PendingResource)
			throw new QuickBuildException("No Way!");
		BuildResource dep = ensureProcessResource(dependent);
		if (dependencies.hasLink(dep, resource))
			return false;
		
		if (debug)
			System.out.println("Adding dependency for " + dependent + " on " + resource);
		dependencies.ensureLink(dep, resource);
//		for (BuildResource br : dependent.buildsResources())
//			ret |= addDependency(br, resource, wantDebug);
		return true;
	}

	private BuildResource ensureProcessResource(Tactic dependent) {
		if (processResources.containsKey(dependent.identifier()))
			return processResources.get(dependent.identifier());
		ProcessResource ret = new ProcessResource(dependent);
		processResources.put(dependent.identifier(), ret);
		dependencies.ensure(ret);
		return ret;
	}

	public void attachStrats(List<Strategem> strats) {
		stratMap = new HashMap<String, Strategem>();
		for (Strategem s : strats) {
			stratMap.put(s.identifier(), s);
			buildOrder.knowAbout(s);
		}
		try
		{
			// existing
			for (BuildResource br : rm.current())
			{
				Node<BuildResource> n = dependencies.find(br);
				if (n.getEntry() instanceof DependencyManager.ComparisonResource)
					dependencies.rename(n, br);
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
						dependencies.rename(n, br);
				}
				for (Tactic t : s.tactics()) {
					BuildResource pr = new ProcessResource(t);
					Node<BuildResource> n = dependencies.find(pr);
					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
						dependencies.rename(n, pr);
				}
			}
			
			// Resolve any clones as best we can ...
			for (CloningResource clone : clones)
			{
				PendingResource pending = clone.getPending();
				BuildResource from = resolve(pending);
				BuildResource actual = from.cloneInto(clone);
				clone.bind(actual);
				Node<BuildResource> n = dependencies.find(actual);
				if (n.getEntry() instanceof DependencyManager.ComparisonResource)
					dependencies.rename(n, actual);
			}

			// needed ones
			for (Strategem s : strats)
			{
				for (PendingResource pr : s.needsResources())
				{
					BuildResource br = resolve(pr);
					Node<BuildResource> n = dependencies.find(br);
					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
						dependencies.rename(n, br);
				}
				for (Tactic tt : s.tactics())
					if (tt instanceof DependencyFloat)
					{
						ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
						for (PendingResource pr : addl) {
							BuildResource br = resolve(pr);
							Node<BuildResource> n = dependencies.find(br);
							if (n.getEntry() instanceof DependencyManager.ComparisonResource)
								dependencies.rename(n, br);
						}
					}
			}
			
			final List<String> fail = new ArrayList<String>();
			dependencies.postOrderTraverse(new NodeWalker<BuildResource>() {

				@Override
				public void present(Node<BuildResource> node) {
					if (node.getEntry() instanceof ComparisonResource) {
						System.out.println("Failed to match " + node.getEntry().compareAs());
						fail.add(node.getEntry().compareAs());
					}
				}
			});
			if (!fail.isEmpty()) {
				System.out.println(dependencies);
				throw new QuickBuildCacheException("Failed to match: " + fail, null);
			}
		}
		catch (Exception ex)
		{
			if (ex instanceof QuickBuildCacheException)
				throw (QuickBuildCacheException)ex;
			throw new QuickBuildCacheException("Failed to attach real strats to cache", ex);
		}
		cache.clear();
	}
}
