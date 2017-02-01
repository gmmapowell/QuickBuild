package com.gmmapowell.quickbuild.build.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.zinutils.reflection.Reflection;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;

public class ApkBuildCommand extends AbstractTactic {

	private final AndroidContext acxt;
	private final File zipFile;
	private final File dexFile;
	private final File keystore; 
	private final File apkFile;
	final ApkResource apkResource;
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();

	public ApkBuildCommand(AndroidCommand parent, AndroidContext acxt, File zipfile, File dexFile, File keystore, File apkFile) {
		super(parent);
		this.acxt = acxt;
		this.zipFile = zipfile;
		this.dexFile = dexFile;
		this.keystore = keystore;
		this.apkFile = apkFile;
		this.apkResource = new ApkResource(this, apkFile);
		this.builds.add(apkResource);
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Class<?> apkClz = acxt.getAPKBuilder();
			String debugStoreOsPath = keystore.getPath();
			PrintStream verboseStream;
			if (showDebug)
				verboseStream = System.out;
			else {
				verboseStream = new PrintStream(baos);
			} 
			Object apkBuilder = Reflection.create(apkClz, apkFile, zipFile, dexFile, debugStoreOsPath, verboseStream);
			Reflection.call(apkBuilder, "sealApk");
			cxt.builtResource(apkResource);
			return BuildStatus.SUCCESS;
		} catch (Exception ex) {
			System.out.println(new String(baos.toByteArray()));
			ex.printStackTrace(System.out);
			return BuildStatus.BROKEN;
		}
	}

	@Override
	public String toString() {
		return "apk builder: " + FileUtils.makeRelative(apkFile);
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "apk");
	}

	public ApkResource getResource() {
		return apkResource;
	}
	
	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}
}
