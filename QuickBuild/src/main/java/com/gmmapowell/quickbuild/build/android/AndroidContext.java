package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;

public class AndroidContext {
	private final File aapt;
	private final File dx;
	private final File jack;
	private final File jill;
	private final File platformJar;
	private final File supportJar;
	private final File apk;
	private final File adb;
	private final String androidPlatform;
	private final String androidBuild;
	private final URLClassLoader apkLoader;
	private List<String> devices;

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
		androidPlatform = conf.getVar("androidplatform");
		androidBuild = conf.getVar("androidbuild");
		File platformDir = FileUtils.fileConcat(androidSDK.getPath(), "platforms", getAndroidPlatform());
		aapt = new File(androidSDK, "build-tools/" + androidBuild + "/aapt" + exe);
		if (!aapt.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + aapt);
		dx = new File(androidSDK, "build-tools/" + androidBuild + "/dx" +bat);
		if (!dx.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + dx);
		jack = new File(androidSDK, "build-tools/" + androidBuild + "/jack.jar");
		if (!jack.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + jack);
		jill = new File(androidSDK, "build-tools/" + androidBuild + "/jill.jar");
		if (!jill.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + jill);
		try {
			String sdklib = conf.getVarIfDefined("androidsdklib", "sdklib-26.0.0-dev.jar");
			apk = new File(androidSDK, "tools/lib/" + sdklib);
			if (!apk.exists())
				throw new QBConfigurationException("Invalid android configuration: cannot find " + apk);
			apkLoader = new URLClassLoader(new URL[] { apk.toURI().toURL() }, this.getClass().getClassLoader());
		} catch (MalformedURLException ex) {
			throw new QBConfigurationException(ex.getMessage());
		}
		adb = new File(androidSDK, "platform-tools/adb" +exe);
		if (!adb.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + adb);
		platformJar = new File(platformDir, "android.jar");
		if (!platformJar.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + platformJar);
		File supportDir = FileUtils.fileConcat(androidSDK.getPath(), "extras", "android", "support", "v13");
		supportJar = new File(supportDir, "android-support-v13.jar");
		if (!supportJar.exists())
			throw new QBConfigurationException("Invalid android configuration: cannot find " + supportJar);
	}

	public File getAAPT() {
		return aapt;
	}
	
	public File getDX() {
		return dx;
	}
	
	public File getJack() {
		return jack;
	}
	
	public File getJill() {
		return jill;
	}
	
	public Class<?> getAPKBuilder() {
		try {
			return Class.forName("com.android.sdklib.build.ApkBuilder", true, apkLoader);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	public File getPlatformJar() {
		return platformJar;
	}

	public File getSupportJar() {
		return supportJar;
	}

	public File getADB() {
		return adb;
	}

	public String getAndroidPlatform() {
		return androidPlatform;
	}

	public List<String> getConnectedDeviceList() {
		if (devices != null)
			return devices;
		
		RunProcess proc = new RunProcess(adb.getPath());
		proc.captureStdout();
		proc.captureStderr();
		proc.arg("devices");
		proc.execute();
		
		String[] s = proc.getStdout().split("\n");
		ArrayList<String> ret = new ArrayList<String>();
		for (int i=1;i<s.length;i++) {
			String[] tmp = s[i].split("[ \t]");
			ret.add(tmp[0]);
		}

		return ret;
	}

}
