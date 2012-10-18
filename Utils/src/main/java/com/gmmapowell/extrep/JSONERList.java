package com.gmmapowell.extrep;

import org.codehaus.jackson.JsonGenerator;

import com.gmmapowell.exceptions.UtilException;

class JSONERList extends ERList {
	private final JsonGenerator gen;

	JSONERList(JsonGenerator gen) {
		this.gen = gen;
	}

	void done() throws Exception {
		gen.writeStartArray();
		for (Object o : children) {
			if (o instanceof String) {
				gen.writeString(o.toString());
			} else if (o instanceof ERObject)
				((JSONERObject)o).done();
			else
				throw new UtilException("Cannot handle " + o.getClass());
		}
		gen.writeEndArray();
	}

	@Override
	protected ERObject newObject(String tag) {
		return new JSONERObject(gen);
	}
}
