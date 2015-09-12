package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

public class Arguments {
	public boolean allTests;
	public boolean buildAll;
	public boolean blank;
	public boolean checkGit = true;
	public boolean configOnly;
	public boolean debug;
	public boolean doubleQuick;
	public boolean ignoreMain;
	public boolean quiet;
	public boolean gfMode = false;
	public boolean readHome = true;
	public boolean teamcity;
	public int nthreads;
	public String file;
	public String cachedir;
	public String upTo;
	public List<String> showArgsFor = new ArrayList<String>();
	public List<String> showDebugFor = new ArrayList<String>();
}
