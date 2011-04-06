package com.gmmapowell.bytecode;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.utils.FileUtils;


public class MethodCreator extends MethodInfo {
	private final String name;
	private final List<String> arguments = new ArrayList<String>();
	private String returnType = "V";

	public MethodCreator(ByteCodeFile bcf, String name) {
		super(bcf);
		this.name = name;
		nameIdx = bcf.requireUtf8(name);
	}

	public void complete() {
		if (access_flags == -1)
			access_flags = ByteCodeFile.ACC_PUBLIC;
		descriptorIdx = bcf.requireUtf8(signature());
	}

	private String signature() {
		StringBuilder sb = new StringBuilder("(");
		for (String s : arguments)
			sb.append(s);
		sb.append(")");
		sb.append(returnType);
		return sb.toString();
	}

	public void argument(String type) {
		arguments.add(map(type));
	}

	private String map(String type) {
		int dims = 0;
		while (type.charAt(dims) == '[')
			dims++;
		return type.substring(0, dims) + mapScalar(type.substring(dims));
	}
	
	private String mapScalar(String type)
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
