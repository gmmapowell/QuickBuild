package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.utils.FileUtils;

public class AndroidContext {

	private final File aapt;
	private final File platformJar;

	public AndroidContext(String androidSDK, String androidPlatform) {
		File platformDir = FileUtils.fileConcat(androidSDK, "platforms", androidPlatform);
		aapt = new File(platformDir, "tools/aapt.exe");
		if (!aapt.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + aapt);
		platformJar = new File(platformDir, "android.jar");
		if (!platformJar.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + platformJar);
	}

	public File getAAPT() {
		return aapt;
	}

	public File getPlatformJar() {
		return platformJar;
	}

}
