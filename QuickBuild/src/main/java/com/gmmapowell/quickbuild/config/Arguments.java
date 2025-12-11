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
	public boolean why;
	public boolean doubleQuick;
	public boolean ignoreMain;
	public boolean quiet;
	public boolean gfMode = false;
	public boolean readHome = true;
	public boolean teamcity;
	public boolean testAlways = false;
	public int nthreads;
	public String file;
	public String cachedir;
	public String upTo;
	public boolean showTimings = false;
	public List<String> showArgsFor = new ArrayList<String>();
	public List<String> showDebugFor = new ArrayList<String>();
}
