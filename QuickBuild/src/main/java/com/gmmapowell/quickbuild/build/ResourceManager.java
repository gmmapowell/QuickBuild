package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class ResourceManager implements ResourceListener {
	private final Config conf;
	private final Set<BuildResource> availableResources = new HashSet<BuildResource>();
	private final List<Notification> notifications = new ArrayList<Notification>();

	public ResourceManager(Config conf) {
		this.conf = conf;
	}

	public void tellMeAbout(Nature nature, Class<? extends BuildResource> cls) {
		notifications.add(new Notification(cls, nature));
	}

	public void configure(List<Strategem> strats) {
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
	}

	@Override
	public void resourceAvailable(BuildResource r) {
		availableResources.add(r);
		if (!r.getPath().exists())
			throw new QuickBuildException("The resource " + r.compareAs() + " has been made available but does not exist");
		for (Notification n : notifications)
			n.dispatch(r);
	}
	
	public Collection<BuildResource> current() {
		return availableResources;
	}
	
	public BuildResource getPendingResource(PendingResource pending) {
		BuildResource ret = getPendingResourceIfAvailable(pending);
		if (ret != null)
			return ret;
		
		String resourceName = pending.compareAs();
		System.out.println("Resource " + resourceName + " not found.  Available Resources are:");
		for (BuildResource s : availableResources)
			System.out.println("  " + s.compareAs());

		throw new UtilException("There is no resource called " + resourceName);
	}

	@SuppressWarnings("unchecked")
	public <T extends BuildResource> T getBuiltResource(Strategem p, Class<T> ofCls) {
		for (BuildResource br : p.buildsResources())
			if (ofCls.isInstance(br))
				return (T)br;
		throw new QuickBuildException("There is no resource of type " + ofCls + " produced by " + p.identifier());
	}

	BuildResource getPendingResourceIfAvailable(PendingResource pending) {
		String resourceName = pending.compareAs();

		// TODO: there really shouldn't be this duplication !!!
		// This time I'm not going to worry about uniqueness
		Pattern p = Pattern.compile(".*" + resourceName.toLowerCase()+".*");
		for (BuildResource br : availableResources)
			if (p.matcher(br.compareAs().toLowerCase()).matches())
				return br;
		
		return null;
	}

	public Iterable<BuildResource> getResources(Class<? extends BuildResource> ofType)
	{
		List<BuildResource> ret = new ArrayList<BuildResource>();
		for (BuildResource br : availableResources)
			if (ofType.isInstance(br))
				ret.add(br);
		return ret;
	}

	public boolean isResourceAvailable(BuildResource br) {
		return availableResources.contains(br);
	}

	public void stratComplete(BuildStatus ret, Strategem strat)
	{
		// Test the contract when the strategem comes to an end
		if (ret.builtResources())
		{
			List<BuildResource> fails = new ArrayList<BuildResource>();
			for (BuildResource br : strat.buildsResources())
				if (!isResourceAvailable(br))
					fails.add(br);
			if (!fails.isEmpty())
			{
				System.out.println("The strategem " + strat + " failed in its contract to build " + fails);
				throw new QuickBuildException("The strategem " + strat + " failed in its contract to build " + fails);
			}
		}
	}

	public void exportAll(ExecuteStrategem strat) {
		for (BuildResource br : strat.getStrat().buildsResources())
			resourceAvailable(br);
	}
}
