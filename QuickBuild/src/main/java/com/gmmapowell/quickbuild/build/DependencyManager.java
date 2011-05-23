package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.graphs.DependencyGraph;
import com.gmmapowell.graphs.Link;
import com.gmmapowell.graphs.Node;
import com.gmmapowell.graphs.NodeWalker;
import com.gmmapowell.quickbuild.build.BuildContext.ComparisonResource;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourceListener;
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
public class DependencyManager implements ResourceListener {
	private final Config conf;
	private final List<Notification> notifications = new ArrayList<Notification>();
	private final DependencyGraph<BuildResource> dependencies = new DependencyGraph<BuildResource>();
	final Map<String, BuildResource> availableResources = new TreeMap<String, BuildResource>();
	private final File dependencyFile;
	private final BuildOrder buildOrder;

	public DependencyManager(Config conf, BuildOrder buildOrder)
	{
		this.conf = conf;
		this.buildOrder = buildOrder;
		dependencyFile = new File(conf.getCacheDir(), "dependencies.xml");
	}
	
	public void figureOutDependencies(List<Strategem> strats)
	{
		// First off, build up a picture of what exists without prompting ...

		// Initial resources are things like pre-built libraries, that
		// we never touch.  They just are.
		conf.tellMeAboutInitialResources(this);

		// Find all the pre-existing items that strategems "produce" without effort ...
		// (e.g. source code artifacts, resources, etc.)
		for (Strategem s : strats)
		{			
			for (BuildResource br : s.providesResources())
			{
				// put it in the graph and note its existence for users ...
				resourceAvailable(br);
			}
		}

		// OK, everything we've seen so far is built at the beginning of time ...
		Set<BuildResource> preBuilt = new HashSet<BuildResource>();
		preBuilt.addAll(availableResources.values());
		
		// Now, separately, let's look at what we could build, if we tried ...
		Set<BuildResource> willBuild = new HashSet<BuildResource>();
		for (Strategem s : strats)
		{
			// Get it in the build order (at level 0)
			buildOrder.depends(s, null);
			for (BuildResource br : s.buildsResources())
			{
				// I'm commenting this out because I don't think that's a good path for something I should do right here, right now.
//				conf.willBuild(br);
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


	public void tellMeAbout(Nature nature, Class<? extends BuildResource> cls) {
		notifications.add(new Notification(cls, nature));
	}

	@Override
	public void resourceAvailable(BuildResource r) {
		availableResources.put(r.compareAs(), r);
		dependencies.ensure(r);
		
		for (Notification n : notifications)
			n.dispatch(r);
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
				BuildResource target = new ComparisonResource(from);
				dependencies.ensure(target);
				for (XMLElement r : e.elementChildren())
				{
					String resource = r.get("resource");
					Node<BuildResource> source = dependencies.find(new ComparisonResource(resource));
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

	boolean isResourceAvailable(BuildResource br)
	{
		if (br instanceof PendingResource)
			return getPendingResourceIfAvailable((PendingResource) br) != null;
		else if (br instanceof CloningResource)
			br = ((CloningResource)br).clonedAs();
		return availableResources.containsKey(br.compareAs());
	}
	
	BuildResource getPendingResourceIfAvailable(PendingResource pending) {
		String resourceName = pending.compareAs();
		if (availableResources.containsKey(resourceName))
			return availableResources.get(resourceName);
		
		// This time I'm not going to worry about uniqueness
		Pattern p = Pattern.compile(".*" + resourceName.toLowerCase()+".*");
		for (BuildResource br : availableResources.values())
			if (p.matcher(br.compareAs().toLowerCase()).matches())
				return br;
		
		return null;
	}

	public Iterable<BuildResource> getResources(Class<? extends BuildResource> ofType)
	{
		List<BuildResource> ret = new ArrayList<BuildResource>();
		for (BuildResource br : availableResources.values())
			if (ofType.isInstance(br))
				ret.add(br);
		return ret;
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

	public BuildResource getPendingResource(PendingResource pending) {
		BuildResource ret = getPendingResourceIfAvailable(pending);
		if (ret != null)
			return ret;
		
		String resourceName = pending.compareAs();
		System.out.println("Resource " + resourceName + " not found.  Available Resources are:");
		for (String s : availableResources.keySet())
			System.out.println("  " + s);

		throw new UtilException("There is no resource called " + resourceName);
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
	
	/* OOD
	private void moveUp(Strategem current, Strategem required) {
		for (int idx=strategemToExecute+1;idx<strats.size();idx++)
		{
			if (strats.get(idx).getBuiltBy() == required)
			{
				StrategemResource sr = strats.remove(idx);
				strats.add(strategemToExecute, sr);
				currentCommands = null;
				moveOn = false;
				repeat = null;
				return;
			}
		}
	}
*/
	Iterable<Strategem> figureDependentsOf(BuildResource node) {
		/* TODO: this implementation is bogus ... 
		Set<StrategemResource> ret = new HashSet<StrategemResource>();
		figureDependentsOf(ret, node);
		for (int i=strategemToExecute+1;i<strats.size();i++)
			if (strats.get(i).getBuiltBy().onCascade())
				ret.add(strats.get(i));
		return ret;
		*/
		return null;
	}

	private void figureDependentsOf(Set<ExecuteStrategem> ret,	BuildResource node) {
		Node<BuildResource> find = dependencies.find(node);
		for (Link<BuildResource> l : find.linksTo())
		{
			Node<BuildResource> n = l.getFromNode();
			BuildResource entry = n.getEntry();
			if (entry instanceof ExecuteStrategem && !ret.contains(entry))
			{
				ret.add((ExecuteStrategem) entry);
			}
			figureDependentsOf(ret, entry);
		}
	}

	/* TODO: reintegrate this - I still want it
	public void figureDirtyness(ExecuteStrategem node, boolean buildAll) {
		Strategem s = node.getBuiltBy();
		OrderedFileList files = s.sourceFiles();
		boolean isDirty;
		if (files == null)
		{
			System.out.println("   **** NULL FILE LIST IN " + node +  " ***");
			isDirty = true;
		}
		else
			isDirty = GitHelper.checkFiles(node.isClean() && !buildAll, files, getGitCacheFile(node));
		if (isDirty || buildAll)
		{
			if (buildAll)
				System.out.println("Marking " + node + " dirty due to --build-all");
			else
				System.out.println("Marking " + node + " dirty due to git hash-object");
			node.markDirty();
			for (Strategem d : figureDependentsOf(node))
			{
				System.out.println("  Marking " + d + " dirty as a dependent");
				buildOrder.markDirty(d);
			}
		}
	}
	*/

	File getGitCacheFile(ExecuteStrategem node) {
		return new File(conf.getCacheDir(), FileUtils.clean(node.name()));
	}
}
