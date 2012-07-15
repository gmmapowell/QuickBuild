package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.JavaInfo.Access;

public interface ByteCodeSink {

	String getCreatedName();

	void superclass(String string);

	byte[] generate();

	MethodDefiner ctor();

	MethodDefiner sctor();

	MethodDefiner method(boolean isStatic, String returns, String name);

	String getSuperClass();

	void defineField(boolean isFinal, Access access, String type, String name);

	void defineField(boolean isFinal, Access access, JavaType type, String name);

	void inheritsClass(String clz);

	void inheritsField(boolean isFinal, Access access, JavaType ofType, String name);

	FieldExpr getField(NewMethodDefiner meth, String name);
	FieldExpr getField(NewMethodDefiner meth, Expr on, String name);

	void makeAbstract();

	void makeInterface();

	void implementsInterface(String intf);

	void signatureAttribute(String name, String sig);

	void addAttribute(String name, byte[] data);

	Annotation addRTVAnnotation(String attrClass);

	Annotation newAnnotation(String attrClass);

	void addInnerClassReference(Access access, String parentClass, String inner);

}