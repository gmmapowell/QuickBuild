package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.collections.ListMap;

public class FieldInfo extends JavaInfo implements AnnotationHolder {
	private final ByteCodeFile bcf;
	private int access_flags;
	private short name_idx;
	private short descriptor_idx;
	final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
	private final ListMap<AnnotationType, Annotation> annotations = new ListMap<AnnotationType, Annotation>(AnnotationType.sortOrder);

	public FieldInfo(ByteCodeFile bcf, boolean isFinal, Access access, String type, String var) {
		this.bcf = bcf;
		int flags = access.asShort();
		if (isFinal)
			flags |= ByteCodeFile.ACC_FINAL;
		access_flags = flags;
		this.name_idx = bcf.pool.requireUtf8(var);
		this.descriptor_idx = bcf.pool.requireUtf8(map(type));
	}

	public FieldInfo(ByteCodeFile bcf) {
		this.bcf = bcf;
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(access_flags);
		dos.writeShort(name_idx);
		dos.writeShort(descriptor_idx);
		bcf.writeAttributes(dos, attributes);
	}

	public void complete() throws IOException {
		for (AnnotationType at : annotations)
		{
			at.addTo(bcf, attributes, annotations.get(at), -1);
		}
	}

	public Annotation addRTVAnnotation(String attrClass) {
		return addAnnotation(AnnotationType.RuntimeVisibleAnnotations, new Annotation(bcf, attrClass));
	}

	public void attribute(String named, String text) {
		short ptr = bcf.pool.requireUtf8(text);
		byte[] data = new byte[2];
		data[0] = (byte)(ptr>>8);
		data[1] = (byte)(ptr&0xff);
		attributes.add(bcf.newAttribute(named, data));
	}

	@Override
	public Annotation addAnnotation(AnnotationType type, Annotation ann) {
		annotations.add(type, ann);
		return ann;
	}
}
