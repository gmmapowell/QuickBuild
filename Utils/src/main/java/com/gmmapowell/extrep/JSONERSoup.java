package com.gmmapowell.extrep;

import org.codehaus.jackson.JsonGenerator;

class JSONERSoup extends ERSoup {

	private final JsonGenerator gen;

	JSONERSoup(JsonGenerator gen) {
		this.gen = gen;
	}

	public ERObject addObject(String tag) {
		JSONERObject obj = new JSONERObject(gen);
		ERObject ret = obj.addObject(tag, tag);
		children.add(obj);
		return ret;
	}

	public void done() throws Exception {
		gen.writeFieldName("_soup");
		gen.writeStartArray();
		for (ERObject o : children) {
			((JSONERObject)o).done();
		}
		gen.writeEndArray();
	}
}
