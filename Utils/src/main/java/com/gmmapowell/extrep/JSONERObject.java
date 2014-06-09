package com.gmmapowell.extrep;

import java.io.OutputStream;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import org.zinutils.exceptions.UtilException;

public class JSONERObject extends ERObject {
	private JsonGenerator gen;

	public JSONERObject(OutputStream out) {
		super(null);
		try {
			JsonFactory jf = new JsonFactory();
			gen = jf.createJsonGenerator(out);
			gen.setPrettyPrinter(new DefaultPrettyPrinter());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	JSONERObject(JsonGenerator gen) {
		super(null);
		this.gen = gen;
	}

	@Override
	public void done() {
		try {
			gen.writeStartObject();
			for (Map.Entry<String,Object> kv : attrs.entrySet()) {
				gen.writeFieldName(kv.getKey());
				gen.writeString((String) kv.getValue());
			}
			for (Map.Entry<String,Object> kv : elements.entrySet()) {
				gen.writeFieldName(kv.getKey());
				Object value = kv.getValue();
				if (value instanceof JSONERObject)
					((JSONERObject)value).done();
				else if (value instanceof JSONERList)
					((JSONERList)value).done();
				else
					throw new UtilException("Cannot handle " + value.getClass());
			}
			if (soup != null)
				((JSONERSoup)soup).done();
			gen.writeEndObject();
			gen.flush();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	protected ERObject newObject(String tag) {
		return new JSONERObject(gen);
	}

	@Override
	protected ERList newList() {
		return new JSONERList(gen);
	}

	@Override
	protected ERSoup newSoup() {
		return new JSONERSoup(gen);
	}
}
