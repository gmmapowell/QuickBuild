package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.ResourceListener;
import com.gmmapowell.quickbuild.core.Tactic;
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

	public void configure(List<Tactic> tactics) {
		conf.tellMeAboutInitialResources(this);

		// Find all the pre-existing items that strategems "produce" without effort ...
		// (e.g. source code artifacts, resources, etc.)
		for (Tactic s : tactics)
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
		resourceAvailable(r, true);
	}

	public void resourceAvailable(BuildResource r, boolean analyze) {
		if (r == null)
			return;
		availableResources.add(r);
		if (r.getPath() != null)
		{
			if (!r.getPath().exists())
				throw new QuickBuildException("The resource " + r.compareAs() + " has been made available but does not exist");
			for (Notification n : notifications)
				n.dispatch(r, analyze);
		}
	}
	
	public Collection<BuildResource> current() {
		return availableResources;
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

	public void exportAll(Tactic tactic) {
		for (BuildResource br : tactic.buildsResources())
			resourceAvailable(br, tactic.analyzeExports());
	}
}
