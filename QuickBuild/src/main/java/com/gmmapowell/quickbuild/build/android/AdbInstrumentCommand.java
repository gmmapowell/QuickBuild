package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;

public class AdbInstrumentCommand extends AbstractBuildCommand {
	private String resource;
	private String instrument;
	private AndroidContext acxt;
	private StructureHelper files;
	private PendingResource apk;
	private boolean background;
	private String device;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private AdbInstrumentedResource buildsStarted;
	private final List<String> extras = new ArrayList<String>();

	// Garnered from: http://stackoverflow.com/questions/4567904/how-to-start-an-application-using-android-adb-tools
	// Also, consider: You can also specify actions to be filter by your intent-filters:
	// am start -a com.example.ACTION_NAME -n com.package.name/com.package.name.ActivityName 

	@SuppressWarnings("unchecked")
	public AdbInstrumentCommand(TokenizedLine toks) {
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "installed apk resource"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "instrument", "activity to instrument"),
				new ArgumentDefinition("--bg", Cardinality.OPTION, "background", "run in background"),
				new ArgumentDefinition("--device", Cardinality.OPTION, "device", "specify device"),
				new ArgumentDefinition("-e", Cardinality.ZERO_OR_MORE, "extras", "extras to pass to activity"));
	}

	@Override
	public Strategem applyConfig(Config config) {
		super.handleOptions(config);
		acxt = config.getAndroidContext();
		apk = new PendingResource(resource);
		needs.add(apk);
		buildsStarted = new AdbInstrumentedResource(this, resource.replace("_apk", "_instrumented"));
		builds.add(buildsStarted);
		return this;
	}
	
	@Override
	public String toString() {
		return "adbinstrument " + (buildsStarted != null?buildsStarted.toString():resource.toString()) + " " + instrument;
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return null;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return OrderedFileList.empty();
	}

	@Override
	public String identifier() {
		return "AdbInstrument[" + apk.getPending() + (device != null ? "_" + device : "") + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		AdbCommand cmd = new AdbCommand(acxt, this, files, apk, buildsStarted);
		if (device != null)
			cmd.setDevice(device);
		cmd.background(background);
		cmd.instrument(instrument, extras);
		return cmd.execute(cxt, showArgs, showDebug);
	}
}
