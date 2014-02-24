package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.SolidResource;
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
			/*
			if (stratMap == null)
				throw new UtilException("Cannot ask for strat before attaching them");
			else if (!stratMap.containsKey(builtBy))
				throw new UtilException("There is no strat matching " + builtBy);
			else
			*/
				throw new UtilException("MISSING CODE DM1");
				// return stratMap.get(builtBy);
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

	// The file to load/store the dependencies in 
	private final File dependencyFile;

	// A flag for debugging
	private final boolean debug;
	
	// A pointer to the resource manager
	private final ResourceManager rm;

	// A set of resources we just know exist
	Set<BuildResource> preBuilt = new TreeSet<BuildResource>();

	// A set of all the resources that we have or could build
	private final Set<BuildResource> allResources = new TreeSet<BuildResource>();
	
	// A map from each tactic to the things it specifically depends on
	private final Map<Tactic, Set<BuildResource>> dependencies = new TreeMap<Tactic, Set<BuildResource>>();

	public DependencyManager(Config conf, ResourceManager rm, boolean debug)
	{
		this.rm = rm;
		this.debug = debug;
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
	}
	
	public void figureOutDependencies(List<Tactic> tactics)
	{
		// Clear out any erroneous info from loading cache
		dependencies.clear();
		
		// First off, build up a picture of what exists without prompting ...

		// OK, everything we've seen so far is built at the beginning of time ...
		for (BuildResource br : rm.current())
		{
			preBuilt.add(br);
			allResources.add(br);
		}
		
		// Now, separately, let's look at what we could build, if we tried ...
//		Set<CloningResource> clones = new HashSet<CloningResource>();
		for (Tactic t : tactics)
		{
			for (BuildResource br : t.buildsResources())
			{
				if (br == null)
					continue;
//				if (br instanceof CloningResource)
//				{
//					CloningResource cr = (CloningResource) br;
//					BuildResource actual = cr.getActual();
//					if (actual == null)
//					{
//						clones.add(cr);
//						allResources.add(cr);
//						continue;
//					}
//					br = actual;
//				}

				allResources.add(br);
			}
		}
		
		// Resolve any clones as best we can ...
//		for (CloningResource clone : clones)
//		{
//			PendingResource pending = clone.getPending();
//			BuildResource from = resolve(pending);
//			BuildResource copy = from.cloneInto(clone);
//			clone.bind(copy);
//		}
//		
		// Now wire up the guys that depend on it
		for (Tactic t : tactics)
		{		
//			t.buildsResources().resolveClones();

//			System.out.println("Figuring initial dependencies for tactic  " + t);
			for (PendingResource pr : t.needsResources())
			{
//				System.out.println("Considering " + pr);
				if (pr == null)
					throw new QuickBuildException("The tactic " + t + " has a null 'needed' resource: " + t.needsResources());
				BuildResource actual = resolve(pr);
				addDependency(t, actual, debug);
				
				/** This no longer appears to do anything
				for (BuildResource br : t.buildsResources())
				{
					if (br == null)
						continue;
					if (br instanceof CloningResource)
					{
						CloningResource cr = (CloningResource) br;
						br = cr.getActual();
						if (br == null)
							throw new QuickBuildException("It's an error for a cloning resource to not be resolved by now");
					}
				}
				*/
			}
			/** AdditionalResources are a thing of the past
			for (Tactic tt : s.tactics())
				if (tt instanceof DependencyFloat)
				{
					BuildResource proc = ensureProcessResource(tt);
					ResourcePacket<PendingResource> addl = ((DependencyFloat)tt).needsAdditionalBuiltResources();
					for (PendingResource pr : addl) {
						BuildResource actual = resolve(pr);
						dependencies.ensureLink(proc, actual);
					}
				}
				*/
		}
	}

	BuildResource resolve(PendingResource pr) {
		if (pr.isBound())
			return pr.physicalResource();
		List<BuildResource> uniq = new ArrayList<BuildResource>();
		Pattern p = Pattern.compile(pr.compareAs().toLowerCase().replaceAll("\\.", "\\\\."));
		for (BuildResource br : allResources)
		{
			if (br instanceof PendingResource || br instanceof ComparisonResource)
				continue;
			if (p.matcher(br.compareAs().toLowerCase()).find())
			{
				uniq.add(br);
			}
		}
		if (uniq.size() == 0) {
			StringBuilder sb = new StringBuilder("Could not find any dependency that matched " + pr.compareAs() +"; have:\n");
			for (BuildResource br : allResources)
				sb.append("  " + br + "\n");
			throw new QuickBuildException(sb.toString());
		}
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
		System.out.println("CANNOT LOAD DEPENDENCIES");
//		if (!dependencyFile.canRead())
		{
			throw new QuickBuildCacheException("There was no dependency cache", null);
		}
		/*
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
//				dependencies.ensure(target);
				for (XMLElement r : e.elementChildren())
				{
					if (r.tag().equals("DependsOn")) {
						String resource = r.get("resource");
//						Node<BuildResource> source = dependencies.find(new ComparisonResource(resource, null));
//						dependencies.ensureLink(target, source.getEntry());
					} else
						throw new UtilException("Cannot handle inner tag: " + r.tag());
				}
			}
		}
		catch (Exception ex)
		{
			throw new UtilException("Could not decipher the dependency cache", ex);
		}
			*/
	}

	public void saveDependencies() {
		final XML output = XML.create("1.0", "Dependencies");
		for (Entry<Tactic, Set<BuildResource>> node : dependencies.entrySet()) {
			Tactic t = node.getKey();
			XMLElement tx = output.addElement("Tactic");
			tx.setAttribute("name", t.identifier());
			
			for (BuildResource br : node.getValue())
			{
				XMLElement ref = tx.addElement("Resource");
				ref.setAttribute("resource", br.compareAs());
			}
		}
		FileUtils.assertDirectory(dependencyFile.getParentFile());
		output.write(dependencyFile);
	}

	public Iterable<BuildResource> getDependencies(Tactic tactic) {
		if (!dependencies.containsKey(tactic)) {
			dependencies.put(tactic, new TreeSet<BuildResource>());
		}
		return dependencies.get(tactic);
	}

	public Iterable<BuildResource> getTransitiveDependencies(Tactic t) {
		Set<Tactic> lookedAt = new TreeSet<Tactic>();
		Set<BuildResource> ret = new TreeSet<BuildResource>();
		getTransitiveDependencies(lookedAt, ret, t);
		return ret;
	}

	private void getTransitiveDependencies(Set<Tactic> lookedAt, Set<BuildResource> ret, Tactic t) {
		if (lookedAt.contains(t))
			return;
		for (BuildResource br : getDependencies(t)) {
			ret.add(br);
			Tactic by = br.getBuiltBy();
			if (by != null)
				getTransitiveDependencies(lookedAt, ret, by);
		}
		for (Tactic pd : t.getProcessDependencies()) {
			getTransitiveDependencies(lookedAt, ret, pd);
		}
	}

	public Iterable<BuildResource> unBuilt()
	{
		return preBuilt;
	}
	
	public String printableDependencyGraph() {
		return dependencies.toString();
	}

	public boolean addDependency(Tactic dependent, BuildResource resource, boolean wantDebug) {
		if (resource instanceof PendingResource)
			throw new QuickBuildException("Cannot make a tactic dependent on a pending resource");
		if (!dependencies.containsKey(dependent))
			dependencies.put(dependent, new TreeSet<BuildResource>());
		Set<BuildResource> set = dependencies.get(dependent);
		if (set.contains(resource))
			return false;
		if (debug)
			System.out.println("Adding dependency for " + dependent + " on " + resource);
		set.add(resource);
		return true;
	}

	public void attachStrats(List<Tactic> tactics) {
		/*
		stratMap = new HashMap<String, Strategem>();
		for (Strategem s : strats) {
			stratMap.put(s.identifier(), s);
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
*/
			// needed ones
			for (Tactic s : tactics)
			{
				for (PendingResource pr : s.needsResources())
				{
					System.out.println("Resolving " + pr);
					BuildResource br = resolve(pr);
//					Node<BuildResource> n = dependencies.find(br);
//					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
//						dependencies.rename(n, br);
				}
			}
				/*
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
		*/
	}
	
	public void cleanFile() {
		dependencyFile.delete();
	}
}
