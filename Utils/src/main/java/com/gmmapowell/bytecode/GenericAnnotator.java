package com.gmmapowell.bytecode;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.JavaInfo.Access;
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
	private GenericAnnotator(ByteCodeCreator byteCodeCreator, boolean isStatic, String name) {
		this.byteCodeCreator = byteCodeCreator;
		this.name = name;
	}
	
	// This is for classes
	private GenericAnnotator(ByteCodeCreator byteCodeCreator) {
		this.byteCodeCreator = byteCodeCreator;
		name = null;
	}

	public static GenericAnnotator newMethod(ByteCodeCreator byteCodeCreator, boolean isStatic, String name) {
		GenericAnnotator ret = new GenericAnnotator(byteCodeCreator, isStatic, name);
		ret.sb.append("()");
		ret.argPointer = 1;
		return ret;
	}

	public static GenericAnnotator forClass(ByteCodeCreator bcc) {
		return new GenericAnnotator(bcc);
	}
	
	public void parentClass(String cls) {
		parentClass(new JavaType(cls));
	}

	public void parentClass(JavaType jt) {
		sb.append(jt.asGeneric());
		hasGenerics |= jt.isGeneric();
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
		if (name == null)
		{
			 // a class, then
			if (hasGenerics)
				byteCodeCreator.signatureAttribute("Signature", sb.toString());
			return null;
		}
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

	public static FieldInfo createField(ByteCodeCreator clz, boolean isStatic, Access access, JavaType javaType, String name) {
		FieldInfo field = clz.field(isStatic, access, javaType.getActual(), name);
//		StringBuilder sb = new StringBuilder();
//		sb.append(JavaInfo.map(javaType.));
//		sb.deleteCharAt(sb.length()-1);
//		sb.append("<");
//		sb.append(JavaInfo.map(S.id));
//		sb.append(JavaInfo.map(S.object));
//		sb.append(">;");
		field.attribute("Signature", javaType.asGeneric());
		return field;
	}
}
