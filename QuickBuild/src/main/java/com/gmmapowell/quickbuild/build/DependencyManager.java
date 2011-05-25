package com.gmmapowell.quickbuild.build;

import java.io.File;
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
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
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
	public static class ComparisonResource extends SolidResource {
		private final String comparison;
	
		public ComparisonResource(String from) {
			super(null, new File(FileUtils.getCurrentDir(), "unused"));
			this.comparison = from;
		}
	
		@Override
		public Strategem getBuiltBy() {
			throw new UtilException("Not implemented");
		}
	
		@Override
		public File getPath() {
			throw new UtilException("Not implemented");
		}
	
		@Override
		public String compareAs() {
			return comparison;
		}
	
	}

	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	private final File dependencyFile;
	private final BuildOrder buildOrder;
	private final ResourceManager rm;

	public DependencyManager(Config conf, ResourceManager rm, BuildOrder buildOrder)
	{
		this.rm = rm;
		this.buildOrder = buildOrder;
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
		for (Strategem s : strats)
		{
			// Get it in the build order (at level 0)
			buildOrder.depends(s, null);
			for (BuildResource br : s.buildsResources())
			{
				willBuild.add(br);
				
				// TODO: sort this out properly.  We should be able to figure *something* out
				if (!(br instanceof CloningResource))
					dependencies.ensure(br);
			}
		}
		
		for (Strategem s : strats)
		{			
			for (PendingResource pr : s.needsResources())
			{
				dependencies.ensure(pr);
				for (BuildResource br : s.buildsResources())
					dependencies.ensureLink(br, pr);
				BuildResource uniq = null;
				Pattern p = Pattern.compile(".*"+pr.compareAs().toLowerCase()+".*");
				for (BuildResource br : preBuilt)
				{
					if (p.matcher(br.compareAs().toLowerCase()).matches())
					{
						if (uniq != null)
							throw new QuickBuildException("Cannot resolve comparison: " + pr.compareAs() + " matches at least " + uniq.compareAs() + " and" + br.compareAs());
						uniq = br;
					}
				}
				for (BuildResource br : willBuild)
				{
					if (p.matcher(br.compareAs().toLowerCase()).matches())
					{
						if (uniq != null)
							throw new QuickBuildException("Cannot resolve comparison: " + pr.compareAs() + " matches at least " + uniq.compareAs() + " and" + br.compareAs());
						uniq = br;
					}
				}
				if (uniq == null)
					throw new QuickBuildException("Could not find any dependency that matched " + pr.compareAs() +": have " + preBuilt);
				dependencies.ensureLink(pr, uniq);
				
				if (uniq.getBuiltBy() != null)
					buildOrder.depends(s, uniq.getBuiltBy());
			}
		}
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
				BuildResource target = new DependencyManager.ComparisonResource(from);
				dependencies.ensure(target);
				for (XMLElement r : e.elementChildren())
				{
					String resource = r.get("resource");
					Node<BuildResource> source = dependencies.find(new DependencyManager.ComparisonResource(resource));
//					System.out.println(target + " <= " + source);
					dependencies.ensureLink(target, source.getEntry());
				}
			}
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
				dep.setAttribute("from", node.getEntry().compareAs());
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
		Set<BuildResource> ret = new HashSet<BuildResource>();
		for (BuildResource br : dependent.buildsResources())
		{
			findDependencies(ret, br);
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
	}

	public String printableDependencyGraph() {
		return dependencies.toString();
	}

	public boolean addDependency(Strategem dependent, BuildResource resource) {
		// The actual dependency is for the things to be built
		boolean ret = false;
		for (BuildResource br : dependent.buildsResources())
			ret |= addDependency(br, resource);
		return ret;
	}

	private boolean addDependency(BuildResource br, BuildResource resource) {
		if (dependencies.hasLink(br, resource))
			return false;
		
		System.out.println("Added dependency from " + br + " on " + resource);
		dependencies.ensureLink(br, resource);
		buildOrder.depends(br.getBuiltBy(), resource.getBuiltBy());
		return true;
	}
	
	public void attachStrats(List<Strategem> strats) {
		try
		{
			for (BuildResource br : rm.current())
			{
				Node<BuildResource> n = dependencies.find(br);
				if (n.getEntry() instanceof DependencyManager.ComparisonResource)
					n.setEntry(br);
			}
			for (Strategem s : strats)
			{
				for (BuildResource br : allResources(s))
				{
					Node<BuildResource> n = dependencies.find(br);
					if (n.getEntry() instanceof DependencyManager.ComparisonResource)
						n.setEntry(br);
				}
			}
		}
		catch (Exception ex)
		{
			throw new QuickBuildCacheException("Failed to attach real strats to cache", ex);
		}
	}

	private Iterable<BuildResource> allResources(Strategem s) {
		Set<BuildResource> ret = new HashSet<BuildResource>();
		addAll(ret, s.needsResources());
		addAll(ret, s.buildsResources());
		addAll(ret, s.providesResources());
		return ret;
	}

	private void addAll(Set<BuildResource> ret, ResourcePacket<? extends BuildResource> resources) {
		for (BuildResource br : resources)
			ret.add(br);
	}
}
