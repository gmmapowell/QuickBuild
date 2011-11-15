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
	private final File adb;

	// TODO: this needs to be cross-platform (somehow)
	public AndroidContext(Config conf) {
		String os = conf.getVar("os");
		String bat = "";
		String exe = "";
		if (os.equals("windows") || os.equals("win7"))
		{
			bat =".bat";
			exe = ".exe";
		}
		File androidSDK = conf.getPath("androidsdk");
		String androidPlatform = conf.getVar("androidplatform");
		File platformDir = FileUtils.fileConcat(androidSDK.getPath(), "platforms", androidPlatform);
		File aapt1 = new File(androidSDK, "platform-tools/aapt" + exe);
		if (!aapt1.exists())
			aapt1 = new File(platformDir, "tools/aapt" +exe);
		if (!aapt1.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + aapt1);
		aapt = aapt1;
		File dx1 = new File(androidSDK, "platform-tools/dx" +bat);
		if (!dx1.exists())
			dx1 = new File(platformDir, "tools/dx" +bat);
		if (!dx1.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + dx1);
		dx = dx1;
		apk = new File(androidSDK, "tools/apkbuilder" + bat);
		if (!apk.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + apk);
		File adb1 = new File(androidSDK, "tools/adb" +exe);
		if (!adb1.exists())
		{
			adb1 = new File(androidSDK, "platform-tools/adb" +exe);
			if (!adb1.exists())
				throw new QBConfigurationException("Invalid android configuration: cannot find " + adb1);
		}
		adb = adb1;
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
