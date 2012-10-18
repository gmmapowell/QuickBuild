package com.gmmapowell.extrep;

import javax.xml.stream.XMLStreamWriter;

import com.gmmapowell.exceptions.UtilException;

class XMLERList extends ERList {
	private final XMLStreamWriter gen;

	XMLERList(XMLStreamWriter gen) {
		this.gen = gen;
	}

	void done() throws Exception {
		for (Object o : children) {
			if (o instanceof String) {
				gen.writeStartElement("String");
				gen.writeAttribute("value", o.toString());
				gen.writeEndElement();
			} else if (o instanceof ERObject)
				((XMLERObject)o).done();
			else
				throw new UtilException("Cannot handle " + o.getClass());
		}
	}

	@Override
	protected ERObject newObject(String tag) {
		return new XMLERObject(gen, tag);
	}
}
