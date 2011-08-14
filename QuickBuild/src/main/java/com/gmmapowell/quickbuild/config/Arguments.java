package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;


public class Arguments {

	public boolean buildAll;
	public boolean blank;
	public boolean configOnly;
	public boolean debug;
	public String file;
	public String cachedir;
	public List<String> showArgsFor = new ArrayList<String>();
	public List<String> showDebugFor = new ArrayList<String>();
}
