package com.gmmapowell.jsgen;

import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.gmmapowell.exceptions.UtilException;

public class JSBuilder {
	private final StringBuilder sb = new StringBuilder();
	private final JsonFactory jf = new JsonFactory();
	
	public void orb() {
		append("(");
	}
	
	public void crb() {
		append(")");
	}
	
	public void ocb() {
		append("{");
	}
	
	public void ccb() {
		append("}");
	}

	public void osb() {
		append("[");
	}
	
	public void csb() {
		append("]");
	}

	public void ident(String s) {
		if (sb.length() > 0 && !Character.isWhitespace(sb.charAt(sb.length()-1)))
			sb.append(' ');
		append(s);
	}

	public void fieldName(String field) {
		if (field == null)
			throw new UtilException("field name cannot be null");
		else
			writeJSON(field.toString());
		append(":");
	}
	
	public void writeJSON(Object value) {
		try {
			StringWriter sw = new StringWriter();
			JsonGenerator jg = jf.createJsonGenerator(sw);
			writeJSON(jg, value);
			jg.flush();
			append(sw.toString());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static void writeJSON(JsonGenerator jg, Object value) {
		try {
			if (value == null)
				jg.writeNull();
			else if (value instanceof String)
				jg.writeString((String) value);
			else
				throw new UtilException("JSBuilder cannot write a value of type " + value.getClass());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void append(String s) {
		sb.append(s);
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}
