package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.utils.FileUtils;

public class AndroidContext {

	private final File aapt;
	private final File dx;
	private final File platformJar;
	private final File apk;

	// TODO: this needs to be cross-platform (somehow)
	public AndroidContext(String androidSDK, String androidPlatform) {
		File androidDir = new File(androidSDK);
		File platformDir = FileUtils.fileConcat(androidSDK, "platforms", androidPlatform);
		aapt = new File(platformDir, "tools/aapt.exe");
		if (!aapt.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + aapt);
		dx = new File(platformDir, "tools/dx.bat");
		if (!dx.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + dx);
		apk = new File(androidDir, "tools/apkbuilder.bat");
		if (!apk.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + apk);
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

}
