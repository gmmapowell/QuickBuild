package com.gmmapowell.bytecode;

import com.gmmapowell.utils.FileUtils;

public class JavaInfo {
	public static String map(String type) {
		if (type.startsWith("@")) // this is my own annotation to allow pre-mapped types to be passed around
			return type.substring(1);
		int dims = 0;
		while (type.charAt(dims) == '[')
			dims++;
		return type.substring(0, dims) + mapScalar(type.substring(dims));
	}

	private static String mapScalar(String type)
	{
		if (type.equals("void"))
			return "V";
		else if (type.equals("int"))
			return "I";
		else if (type.equals("byte"))
			return "B";
		else if (type.equals("char"))
			return "C";
		else if (type.equals("double"))
			return "D";
		else if (type.equals("float"))
			return "F";
		else if (type.equals("long"))
			return "J";
		else if (type.equals("short"))
			return "S";
		else if (type.equals("boolean"))
			return "Z";
		return "L"+FileUtils.convertDottedToSlashPath(type) +";";
	}

}
