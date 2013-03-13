package com.gmmapowell.bytecode;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.JavaInfo.Access;
import com.gmmapowell.exceptions.UtilException;

public class GenericAnnotator {

	public static class AnnArg {

		private final String name;
		private final String string;
		private final String[] args;
		
		public AnnArg(String name, String value) {
			this.name = name;
			this.string = value;
			this.args = null;
		}

		public AnnArg(String name, String[] args) {
			this.name = name;
			this.string = null;
			this.args = args;
		}

	}

	public static class GenAnnotation {
		protected final String attrClass;
		protected final List<AnnArg> args = new ArrayList<AnnArg>();

		public GenAnnotation(String attrClass) {
			this.attrClass = attrClass;
		}

		public void addParam(String string, String value) {
			args.add(new AnnArg(string, value));
		}

		public void addParam(String string, String... strings) {
			args.add(new AnnArg(string, strings));
		}

		public void applyTo(MethodDefiner meth) {
			handleArgs(meth.addRTVAnnotation(attrClass));
		}

		protected void handleArgs(Annotation methAnn) {
			for (AnnArg aa : args)
			{
				if (aa.string != null)
					methAnn.addParam(aa.name, aa.string);
				else
					methAnn.addParam(aa.name, aa.args);
			}
		}

	}

	public static class ArgAnnotation extends GenAnnotation {

		public ArgAnnotation(String pathparam) {
			super(pathparam);
		}

		public void applyTo(MethodDefiner meth, int pos) {
			handleArgs(meth.addRTVPAnnotation(attrClass, pos));
		}
	}

	public static class PendingVar {
		private final JavaType type;
		private final String name;
		private Var var;
		private List<ArgAnnotation> anns = new ArrayList<ArgAnnotation>();
		private final int pos;

		public PendingVar(JavaType type, String name, int pos) {
			this.type = type;
			this.name = name;
			this.pos = pos;
		}
		
		public void apply(MethodDefiner meth) {
			var = ((MethodCreator)meth).argument(type.getActual(), name);
			for (ArgAnnotation aa : anns)
				aa.applyTo(meth, pos);
		}
		
		public Var getVar()
		{
			if (var == null)
				throw new UtilException("Must apply before get()");
			return var;
		}

		public ArgAnnotation addRTVPAnnotation(String pathparam) {
			ArgAnnotation ret = new ArgAnnotation(pathparam);
			anns.add(ret);
			return ret;
		}
	}

	private String returnType;
	private StringBuilder sb = new StringBuilder();
	private int argPointer;
	private boolean hasGenerics;
	private final ByteCodeCreator byteCodeCreator;
	private final String name;
	private List<PendingVar> vars = new ArrayList<PendingVar>();
	private final boolean isStatic;
	private List<GenAnnotation> anns = new ArrayList<GenAnnotation>();

	// This works for method ...
	private GenericAnnotator(ByteCodeSink projectionClass, boolean isStatic, String name) {
		this.byteCodeCreator = (ByteCodeCreator) projectionClass;
		this.isStatic = isStatic;
		this.name = name;
	}
	
	// This is for classes
	private GenericAnnotator(ByteCodeSink projectionClass) {
		this.byteCodeCreator = (ByteCodeCreator) projectionClass;
		isStatic = false;
		name = null;
	}

	public static GenericAnnotator newMethod(ByteCodeSink projectionClass, boolean isStatic, String name) {
		GenericAnnotator ret = new GenericAnnotator(projectionClass, isStatic, name);
		ret.sb.append("()");
		ret.argPointer = 1;
		return ret;
	}

	public static GenericAnnotator newConstructor(ByteCodeSink bcc, boolean isStatic) {
		GenericAnnotator ret;
		if (isStatic)
			ret = newMethod(bcc, true, "<clinit>");
		else
			ret = newMethod(bcc, false, "<init>");
		ret.returns(new JavaType("void"));
		return ret;
	}

	public static GenericAnnotator forClass(ByteCodeSink projectionClass) {
		return new GenericAnnotator(projectionClass);
	}
	
	public void parentClass(String cls) {
		parentClass(new JavaType(cls));
	}

	public void parentClass(JavaType jt) {
		sb.append(jt.asGeneric());
		hasGenerics |= jt.isGeneric();
	}

	public void returns(String str) {
		returns(new JavaType(str));
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
	
	public PendingVar argument(String cs, String name) {
		return argument(new JavaType(cs), name);
	}

	public PendingVar argument(JavaType jt, String name) {
		if (sb  == null)
			throw new UtilException("You cannot continue to use annotator after completion");
		hasGenerics |= jt.isGeneric();
		sb.insert(argPointer, jt.asGeneric());
		argPointer += jt.asGeneric().length();
		PendingVar ret = new PendingVar(jt, name, vars.size());
		vars.add(ret);
		return ret;
	}

	public MethodDefiner done() {
		if (sb == null)
			throw new UtilException("You have already completed this method");
		if (name == null)
		{
			 // a class, then
			if (hasGenerics)
				byteCodeCreator.signatureAttribute("Signature", sb.toString());
			return null;
		}
		if (returnType == null)
			throw new UtilException("You have not specified the return type");
		MethodDefiner ret = byteCodeCreator.createMethod(isStatic, returnType, name);
		if (hasGenerics)
		{
			ret.addAttribute("Signature", sb.toString());
		}
		sb = null;
		
		// Done includes spitting out any captured annotations
		for (GenAnnotation ann : anns)
			ann.applyTo(ret);
		for (PendingVar p : vars)
			p.apply(ret);
		return ret;
	}
	
	public static void annotateField(FieldInfo fi, JavaType jt) {
		if (jt.isGeneric())
			fi.attribute("Signature", jt.asGeneric());
	}

	public static void createField(ByteCodeSink projectionClass, boolean isStatic, Access access, JavaType javaType, String name) {
		projectionClass.defineField(isStatic, access, javaType, name);
	}

	public GenAnnotation addRTVAnnotation(String annotation) {
		GenAnnotation ret = new GenAnnotation(annotation);
		anns.add(ret);
		return ret;
	}
}
