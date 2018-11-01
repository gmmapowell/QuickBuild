package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.quickbuild.build.java.DirectoryResource;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

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
	
	// First off, build up a picture of what exists without prompting ...
	public void init(List<Tactic> tactics)
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
		for (Tactic t : tactics)
		{
			for (BuildResource br : t.buildsResources())
			{
				if (br == null)
					continue;

				allResources.add(br);
			}
		}
	}

	public boolean loadDependencyCache(List<Tactic> tactics)
	{
		for (Tactic t : tactics)
			for (PendingResource pr : t.needsResources())
				resolve(pr);

		if (!dependencyFile.canRead())
			return false;

		try
		{
			final XML input = XML.fromFile(dependencyFile);
			for (XMLElement tx : input.top().elementChildren())
			{
				Tactic t = findTacticByName(tactics, tx.get("name"));
				for (XMLElement rx : tx.elementChildren()) {
					BuildResource dependsOn = findResourceByName(rx.get("name"));
					addDependency(t, dependsOn, debug);
				}
			}
		}
		catch (Exception ex)
		{
			dependencyFile.delete();
			throw new UtilException("Could not decipher the dependency cache", ex);
		}
		return true;
	}

	public void figureOutDependencies(List<Tactic> tactics)
	{
		// Clear out any erroneous info from loading cache
		dependencies.clear();

		// Now wire up the guys that depend on it
		for (Tactic t : tactics)
		{		
//			System.out.println("Figuring initial dependencies for tactic  " + t);
			for (PendingResource pr : t.needsResources())
			{
//				System.out.println("Considering " + pr);
				if (pr == null)
					throw new QuickBuildException("The tactic " + t + " has a null 'needed' resource: " + t.needsResources());
				BuildResource actual = resolve(pr);
				addDependency(t, actual, debug);
			}
		}
	}

	BuildResource resolve(PendingResource pr) {
		if (pr.isBound())
			return pr.physicalResource();
		List<BuildResource> uniq = new ArrayList<BuildResource>();
		Pattern p = Pattern.compile(pr.compareAs().toLowerCase().replaceAll("\\.", "\\\\.").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"));
		for (BuildResource br : allResources)
		{
			if (br instanceof PendingResource)
				continue;
			if (p.matcher(br.compareAs().toLowerCase()).find())
			{
				uniq.add(br);
			}
		}
		if (uniq.size() == 0) {
			// final effort ... is it a dir?
			File asdir = new File(FileUtils.getCurrentDir(), pr.getPending());
			if (asdir.isDirectory()) {
				DirectoryResource r = new DirectoryResource(null, asdir);
				pr.bindTo(r);
				allResources.add(r);
				return r;
			}
			StringBuilder sb = new StringBuilder("Could not find any dependency that matched " + pr.compareAs() +"; have:\n");
			for (BuildResource br : allResources)
				sb.append("  " + br.compareAs() + "\n");
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

	private Tactic findTacticByName(List<Tactic> tactics, String name) {
		for (Tactic t : tactics) {
			if (t.identifier().equals(name))
				return t;
		}
		StringBuilder sb = new StringBuilder("Could not find any tactic that matched " + name +"; have:\n");
		for (Tactic br : tactics)
			sb.append("  " + br.identifier() + "\n");
		throw new QuickBuildException(sb.toString());
	}

	private BuildResource findResourceByName(String name) {
		for (BuildResource br : allResources) {
			if (br.compareAs().equals(name))
				return br;
		}
		StringBuilder sb = new StringBuilder("Could not find any resource that matched " + name +"; have:\n");
		for (BuildResource br : allResources)
			sb.append("  " + br.compareAs() + "\n");
		throw new QuickBuildException(sb.toString());
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
				ref.setAttribute("name", br.compareAs());
			}
		}
		FileUtils.assertDirectory(dependencyFile.getParentFile());
		output.write(dependencyFile);
	}

	public Set<BuildResource> getDependencies(Tactic tactic) {
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
		if (t == null || lookedAt.contains(t))
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

	public void cleanFile() {
		dependencyFile.delete();
	}
}
