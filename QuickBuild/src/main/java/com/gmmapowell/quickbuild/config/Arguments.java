package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;


public class Arguments {

	public boolean buildAll;
	public boolean blank;
	public boolean configOnly;
	public boolean debug;
	public boolean quiet;
	public int nthreads;
	public String file;
	public String cachedir;
	public String upTo;
	public List<String> showArgsFor = new ArrayList<String>();
	public List<String> showDebugFor = new ArrayList<String>();
}
