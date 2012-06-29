package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.collections.ListMap;

public class MethodInfo extends JavaInfo implements AnnotationHolder {
	protected short access_flags = -1;
	protected short nameIdx = -1;
	protected short descriptorIdx = -1;
	protected final ByteCodeFile bcf;
	protected final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
	private ListMap<AnnotationType, Annotation> annotations = new ListMap<AnnotationType, Annotation>();

	public MethodInfo(ByteCodeFile bcf)
	{
		this.bcf = bcf;
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(access_flags);
		dos.writeShort(nameIdx);
		dos.writeShort(descriptorIdx);
		bcf.writeAttributes(dos, attributes);
	}

	public String getName() {
		return ((Utf8Info)bcf.pool.get(nameIdx)).asString();
	}
	
	public String getSignature() {
		return ((Utf8Info)bcf.pool.get(descriptorIdx)).asString();
	}

	@Override
	public String toString() {
		if (nameIdx != -1 && descriptorIdx != -1)
			return "Method[" + bcf.pool.get(nameIdx) + bcf.pool.get(descriptorIdx) +"]";
		else
			return "Method[?,?]";
	}

	public AttributeInfo getAttribute(String string) {
		for (AttributeInfo ai : attributes)
			if (ai.hasName(string))
				return ai;
		return null;
	}
	
	public Annotation getClassAnnotation(String ann) {
		for (AnnotationType i : annotations)
		{
			for (Annotation j : annotations.get(i))
				if (j.name.equals(ann))
					return j;
		}
		return null;
	}

	@Override
	public Annotation addAnnotation(AnnotationType type, Annotation ann) {
		annotations.add(type, ann);
		return ann;
	}
}
