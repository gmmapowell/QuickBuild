package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.exceptions.UtilException;

public enum AnnotationTag {
	ANNOTATION, ARRAY, CLASS, ENUM, TEXT;
	
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
			AnnotationValue[] arr = (AnnotationValue[])value;
			dos.writeShort(arr.length);
			for (AnnotationValue aa : arr)
				aa.write(bcf, dos);
			break;
		}
		case CLASS:
		{
			dos.writeByte('c');
			dos.writeShort((Short)value);
			break;
		}
		case ENUM:
		{
			dos.writeByte('e');
			String[] typeValue = (String[])value;
			dos.writeShort(bcf.pool.requireUtf8((String)typeValue[0]));
			dos.writeShort(bcf.pool.requireUtf8((String)typeValue[1]));
			break;
		}
		case TEXT:
		{
			dos.writeByte('s');
			dos.writeShort(bcf.pool.requireUtf8((String)value));
			break;
		}
		default:
			throw new UtilException("Cannot handle " + this);
		}
	}

	public static AnnotationTag parse(DataInputStream dis) throws IOException {
		char b = (char) dis.readByte();
		switch (b)
		{
		case '@':
			return ANNOTATION;
		case '[':
			return ARRAY;
		case 'c':
			return CLASS;
		case 'e':
			return ENUM;
		case 's':
			return TEXT;
		default:
			throw new UtilException("Cannot handle " + b);
		}
	}

	public Object parseValue(ByteCodeFile bcf, DataInputStream dis) throws IOException {
		switch (this) {
		case ANNOTATION:
		{
			return Annotation.parseOne(bcf, dis);
		}
		case ARRAY:
		{
			int len = dis.readShort();
//			System.out.println("Array len = " + len);
			AnnotationValue[] arr = new AnnotationValue[len];
			for (int i=0;i<len;i++)
				arr[i] = AnnotationValue.parse(bcf, dis);
			return arr;
		}
		case CLASS:
		{
			return dis.readShort();
		}
		case ENUM:
		{
			String type = ((Utf8Info) bcf.pool.get(dis.readShort())).asString();
			String constant = ((Utf8Info) bcf.pool.get(dis.readShort())).asString();
			return new String[] { type, constant };
		}
		case TEXT:
		{
			return ((Utf8Info) bcf.pool.get(dis.readShort())).asString();
		}
		default:
			throw new UtilException("Cannot handle " + this);
		}
	}
}
