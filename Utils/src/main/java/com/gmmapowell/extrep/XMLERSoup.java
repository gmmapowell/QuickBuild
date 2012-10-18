package com.gmmapowell.extrep;

import javax.xml.stream.XMLStreamWriter;

import com.gmmapowell.exceptions.UtilException;

class XMLERSoup extends ERSoup {
	private final XMLStreamWriter gen;

	XMLERSoup(XMLStreamWriter gen) {
		this.gen = gen;
	}

	public ERObject addObject(String tag) {
		ERObject ret = new XMLERObject(gen, tag);
		children.add(ret);
		return ret;
	}

	public void done() throws Exception {
		for (Object o : children) {
			if (o instanceof ERObject)
				((XMLERObject)o).done();
			else
				throw new UtilException("Cannot handle " + o.getClass());
		}
	}
}
