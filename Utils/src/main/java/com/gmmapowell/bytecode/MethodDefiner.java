package com.gmmapowell.bytecode;

import java.io.IOException;

import com.gmmapowell.bytecode.JavaInfo.Access;

public interface MethodDefiner extends NewMethodDefiner {

	void setAccess(Access a);

	void makeFinal();

	void addAttribute(String named, String text);

	// @Deprecated // I would like to deprecate this, but can't pay the cost right now
	// Use GenAnnotator instead
	Var argument(String type);

	void complete() throws IOException;

	void aaload();

	void aconst_null();

	void aload(int i);

	void anewarray(String clz);

	void areturn();

	void astore(int i);

	void athrow();

	void checkCast(String clz);

	void dload(int i);

	void dreturn();

	void dstore(int i);

	void dup();

	void getField(String clz, String type, String var);

	void getStatic(String clz, String type, String var);

	void iconst(int i);

	Marker ifeq();

	Marker ifne();

	Marker iflt();

	Marker ifge();

	Marker ifgt();

	Marker ifle();

	Marker ifnull();

	Marker ifnonnull();

	void iload(int i);

	void istore(int i);

	void invokeOtherConstructor(String clz, String... args);

	void invokeParentConstructor(String... args);

	void invokeParentMethod(String typeReturn, String method, String... args);

	void invokeStatic(String clz, String typeReturn, String method,
			String... args);

	void invokeVirtualMethod(String clz, String ret, String method,
			String... args);

	void invokeInterface(String clz, String ret, String method, String... args);

	void ireturn();
	
	void isInstanceOf(String ofClz);

	Marker jump();

	void ldcClass(String clz);

	void ldcString(String string);

	void lload(int i);
	
	void lreturn();

	void lstore(int id);

	void newObject(String clz);

	void pop(String type);

	void putField(String clz, String type, String var);

	void putStatic(String clz, String type, String var);

	void vreturn();

	Annotation addRTVAnnotation(String attrClass);

	Annotation addRTVPAnnotation(String attrClass, int param);

	int nextLocal();

	String getClassName();

	int stackDepth();

	void resetStack(int to);
}