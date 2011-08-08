package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.utils.FileUtils;

public class AndroidContext {

	private final File aapt;
	private final File dx;
	private final File platformJar;
	private final File apk;
	private File adb;

	// TODO: this needs to be cross-platform (somehow)
	public AndroidContext(Config conf) {
		String os = conf.getVar("os");
		String bat = "";
		if (os.equals("windows") || os.equals("win7"))
			bat =".bat";
		File androidSDK = conf.getPath("androidsdk");
		String androidPlatform = conf.getVar("androidplatform");
		File platformDir = FileUtils.fileConcat(androidSDK.getPath(), "platforms", androidPlatform);
		aapt = new File(platformDir, "tools/aapt");
		if (!aapt.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + aapt);
		dx = new File(platformDir, "tools/dx" +bat);
		if (!dx.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + dx);
		apk = new File(androidSDK, "tools/apkbuilder" + bat);
		if (!apk.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + apk);
		adb = new File(androidSDK, "tools/adb");
		if (!adb.exists())
		{
			adb = new File(androidSDK, "platform-tools/adb");
			if (!adb.exists())
				throw new QBConfigurationException("Invalid android configuration: cannot find " + adb + " in either location");
		}
		platformJar = new File(platformDir, "android.jar");
		if (!platformJar.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + platformJar);
	}

	public File getAAPT() {
		return aapt;
	}
	
	public File getDX() {
		return dx;
	}
	
	public File getAPKBuilder() {
		return apk;
	}

	public File getPlatformJar() {
		return platformJar;
	}

	public File getADB() {
		return adb;
	}

}
