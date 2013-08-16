package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum AnnotationType {
	RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations;

	public static final Comparator<AnnotationType> sortOrder = new Comparator<AnnotationType>() {
		@Override
		public int compare(AnnotationType o1, AnnotationType o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}; 
	
	public boolean isPerParameter() {
		return this == RuntimeVisibleParameterAnnotations;
	}
	
	public void addTo(ByteCodeFile bcf, List<AttributeInfo> attributes, List<Annotation> list, int paramCount) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		if (isPerParameter())
		{
			dos.writeByte(paramCount);
			List<List<Annotation>> lists = new ArrayList<List<Annotation>>();
			for (int i=0;i<paramCount;i++)
				lists.add(new ArrayList<Annotation>());
			for (Annotation a : list)
			{
				lists.get(a.forParam()).add(a);
			}
			for (List<Annotation> al : lists)
				writeAnnoList(dos, al);
		}
		else
			writeAnnoList(dos, list);
		attributes.add(new AttributeInfo(bcf, this.toString(), baos.toByteArray()));
	}

	private void writeAnnoList(DataOutputStream dos, List<Annotation> list) throws IOException {
		dos.writeShort(list.size());
		for (Annotation a : list)
		{
			a.write(dos);
		}
	}
}
