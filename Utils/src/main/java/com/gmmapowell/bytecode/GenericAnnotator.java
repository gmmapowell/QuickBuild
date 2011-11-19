package com.gmmapowell.bytecode;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class GenericAnnotator {

	public static class PendingVar {
		private final JavaType type;
		private final String name;
		private Var var;

		public PendingVar(JavaType type, String name) {
			this.type = type;
			this.name = name;
		}
		
		public void apply(MethodCreator meth) {
			var = meth.argument(type.getActual(), name);
		}
		
		public Var getVar()
		{
			if (var == null)
				throw new UtilException("Must apply before get()");
			return var;
		}
	}

	private String returnType;
	private StringBuilder sb = new StringBuilder();
	private int argPointer;
	private boolean hasGenerics;
	private final ByteCodeCreator byteCodeCreator;
	private final String name;
	private List<PendingVar> vars = new ArrayList<PendingVar>();

	// This works for method ...
	public GenericAnnotator(ByteCodeCreator byteCodeCreator, boolean isStatic, String name) {
		this.byteCodeCreator = byteCodeCreator;
		this.name = name;
	}
	
	public static GenericAnnotator newMethod(ByteCodeCreator byteCodeCreator, boolean isStatic, String name) {
		GenericAnnotator ret = new GenericAnnotator(byteCodeCreator, isStatic, name);
		ret.sb.append("()");
		ret.argPointer = 1;
		return ret;
	}

	public void returns(JavaType jt) {
		if (returnType != null)
			throw new UtilException("You cannot specify more than one return type");
		returnType = jt.getActual();
		if (sb  == null)
			throw new UtilException("You cannot continue to use annotator after completion");
		sb.append(jt.asGeneric());
		hasGenerics |= jt.isGeneric();
	}
	
	public PendingVar argument(JavaType jt, String name) {
		if (sb  == null)
			throw new UtilException("You cannot continue to use annotator after completion");
		hasGenerics |= jt.isGeneric();
		sb.insert(argPointer, jt.asGeneric());
		argPointer += jt.asGeneric().length();
		PendingVar ret = new PendingVar(jt, name);
		vars.add(ret);
		return ret;
	}

	public MethodCreator done() {
		if (sb == null)
			throw new UtilException("You have already completed this class");
		if (returnType == null)
			throw new UtilException("You have not specified the return type");
		MethodCreator ret = byteCodeCreator.method(returnType, name);
		for (PendingVar p : vars)
			p.apply(ret);
		if (hasGenerics)
		{
			ret.addAttribute("Signature", sb.toString());
		}
		sb = null;
		return ret;
	}
	
	public static void annotateField(FieldInfo fi, JavaType jt) {
		fi.attribute("Signature", jt.asGeneric());
		// TODO Auto-generated method stub
		
	}
	/*
	StringBuilder sb = new StringBuilder();
	sb.append("(");
	if (s != null)
		sb.append(JavaInfo.map(S.string));
	sb.append(JavaInfo.map(type));
	sb.deleteCharAt(sb.length()-1);
	sb.append("<");
	sb.append(JavaInfo.map(type));
	sb.append(">;)V");
	meth.addAttribute("Signature", sb.toString());
	*/
}
