package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.exceptions.UtilException;

public enum AnnotationTag {
	ANNOTATION, ARRAY, CLASS, TEXT;
	
	public void write(ByteCodeFile bcf, DataOutputStream dos, Object value) throws IOException {
		switch (this) {
		case ANNOTATION:
		{
			dos.writeByte('@');
			((Annotation)value).write(dos);
			break;
		}
		case ARRAY:
		{
			dos.writeByte('[');
			AnnotationArg[] arr = (AnnotationArg[])value;
			dos.writeShort(arr.length);
			for (AnnotationArg aa : arr)
				aa.tag.write(bcf, dos, aa.value);
			break;
		}
		case CLASS:
		{
			dos.writeByte('c');
			dos.writeShort((Short)value);
			break;
		}
		case TEXT:
		{
			dos.writeByte('s');
			dos.writeShort(bcf.requireUtf8((String)value));
			break;
		}
		default:
			throw new UtilException("Cannot handle " + this);
		}
	};
}