package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.exceptions.UtilException;

public class AnnotationValue {

	private final AnnotationTag tag;
	private final Object value;

	public AnnotationValue(AnnotationTag tag, Object value) {
		this.tag = tag;
		this.value = value;
	}

	public static AnnotationValue parse(ByteCodeFile bcf, DataInputStream dis) throws IOException {
		AnnotationTag tag = AnnotationTag.parse(dis);
		Object value = tag.parseValue(bcf, dis);
		return new AnnotationValue(tag, value);
	}
	
	public void write(ByteCodeFile bcf, DataOutputStream dos) throws IOException {
		tag.write(bcf, dos, value);
	}
	
	public String asString() {
		if (tag == AnnotationTag.TEXT)
			return (String) value;
		else if (tag == AnnotationTag.ENUM)
			return ((String[])value)[1];
		throw new UtilException("Not a string");
	}
	
	@Override
	public String toString() {
		return tag + "."+ value;
	}

	public AnnotationValue[] asArray() {
		if (!(value instanceof AnnotationValue[]))
			throw new UtilException("The annotation value is not an array of annotations");
		return (AnnotationValue[]) value;
	}

	public boolean isString() {
		return tag == AnnotationTag.TEXT || tag == AnnotationTag.ENUM;
	}

	public boolean isArray() {
		return tag == AnnotationTag.ARRAY;
	}

	public Annotation asAnnotation() {
		if (!(value instanceof Annotation))
			throw new UtilException("The annotation value is not an annotation");
		return (Annotation)value;
	}
}
