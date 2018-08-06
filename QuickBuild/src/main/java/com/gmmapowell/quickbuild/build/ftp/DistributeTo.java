package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;

public interface DistributeTo {

	BuildStatus distribute(boolean showDebug, File local, String remote) throws Exception;

	BuildResource resource(DistributeCommand cmd);

}
