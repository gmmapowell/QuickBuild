package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zinutils.exceptions.UtilException;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;

public class DistributeToS3 implements DistributeTo {
	private boolean fullyConfigured;
	private File privateKeyPath;
	private String bucket;
	private String host;
	private String saveAs;
	private String directory;

	public DistributeToS3(Config config, String directory, String dest) {
		this.directory = directory;
		fullyConfigured = true;
		if (config.hasPath("awspath"))
			privateKeyPath = config.getPath("awspath");
		else
		{
			System.out.println("Cannot s3: no aws setup");
			fullyConfigured = false;
		}
		Pattern p = Pattern.compile("s3:([a-zA-Z0-9_]+)(.s3.amazonaws.com)/(.+)");
		Matcher matcher = p.matcher(dest);
		if (!matcher.matches())
			throw new UtilException("Could not match s3 path " + dest);
		
		bucket = matcher.group(1);
		host = matcher.group(1)+matcher.group(2);
		saveAs = matcher.group(3);
	}

	@Override
	public BuildStatus distribute(boolean showDebug, File local, String remote) throws IOException {
		if (!fullyConfigured)
			return BuildStatus.SKIPPED;
		AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(privateKeyPath));
		if (!local.isDirectory())
			s3.putObject(bucket, saveAs + remote, local);
		return BuildStatus.SUCCESS;
	}

	@Override
	public BuildResource resource(DistributeCommand cmd) {
		return new DistributeResource(cmd, directory, host);
	}
}
