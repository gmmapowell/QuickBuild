package com.gmmapowell.bytecode;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class JavaInfo {
	public static enum Access { PRIVATE, PUBLIC, PROTECTED, DEFAULT, PROTECTEDTRANSIENT, PRIVATESTATIC, PUBLICSTATIC, DEFAULTSTATIC, PUBLICTRANSIENT, PUBLICABSTRACT, ACCESS, STATICACCESS, ENUM; 
		public short asShort() {
			switch (this)
			{
			case PRIVATE:
				return ByteCodeFile.ACC_PRIVATE;
			case PRIVATESTATIC:
				return ByteCodeFile.ACC_PRIVATE|ByteCodeFile.ACC_STATIC;
			case PROTECTED:
				return ByteCodeFile.ACC_PROTECTED;
			case PROTECTEDTRANSIENT:
				return ByteCodeFile.ACC_PROTECTED | ByteCodeFile.ACC_TRANSIENT;
			case DEFAULT:
				return 0;
			case DEFAULTSTATIC:
				return ByteCodeFile.ACC_STATIC;
			case PUBLIC:
				return ByteCodeFile.ACC_PUBLIC;
			case PUBLICSTATIC:
				return ByteCodeFile.ACC_PUBLIC|ByteCodeFile.ACC_STATIC;
			case PUBLICTRANSIENT:
				return ByteCodeFile.ACC_PUBLIC|ByteCodeFile.ACC_TRANSIENT;
			case PUBLICABSTRACT:
				return ByteCodeFile.ACC_PUBLIC|ByteCodeFile.ACC_ABSTRACT;
			case ACCESS:
				return ByteCodeFile.ACC_ACCESSMETH;
			case STATICACCESS:
				return ByteCodeFile.ACC_ACCESSMETH|ByteCodeFile.ACC_STATIC;
			case ENUM:
				return ByteCodeFile.ACC_FINAL|ByteCodeFile.ACC_PUBLIC|ByteCodeFile.ACC_STATIC|ByteCodeFile.ACC_ENUM;
			default:
				throw new UtilException("Invalid access");
			}
		}
	};

	public static String mapPrimitive(String type) {
		return mapInternal(type, false);
	}

	public static String map(String type) {
		return mapInternal(type, true);
	}

	private static String mapInternal(String type, boolean mapLongTypes) {
		if (type.startsWith("@")) // this is my own annotation to allow pre-mapped types to be passed around
			return type.substring(1);
		if (type.equals("?"))
			return "*";
		int dims = 0;
		while (type.charAt(dims) == '[')
			dims++;
		return type.substring(0, dims) + mapScalar(type.substring(dims), mapLongTypes);
	}

	public static String unmap(String mapped) {
		if (mapped.startsWith("@"))
			return unmap(mapped.substring(1));
		if (mapped.equals("*"))
			return "?";
		int dims = 0;
		while (mapped.charAt(dims) == '[')
			dims++;
		return mapped.substring(0, dims) + unmapScalar(mapped.substring(dims));
	}

	private static String mapScalar(String type, boolean mapLongTypes)
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
		else if (mapLongTypes)
			return "L"+FileUtils.convertDottedToSlashPath(type) +";";
		else
			return type;
	}

	private static String unmapScalar(String mapped)
	{
		if (mapped.equals("V"))
			return "void";
		else if (mapped.equals("I"))
			return "int";
		else if (mapped.equals("B"))
			return "byte";
		else if (mapped.equals("C"))
			return "char";
		else if (mapped.equals("D"))
			return "double";
		else if (mapped.equals("F"))
			return "float";
		else if (mapped.equals("J"))
			return "long";
		else if (mapped.equals("S"))
			return "short";
		else if (mapped.equals("Z"))
			return "boolean";
		return FileUtils.convertToDottedName(new File(mapped.substring(1, mapped.length()-1)));
	}

}
