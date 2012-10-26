package com.gmmapowell.jsgen;

import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.PrettyPrinter;

public class JSBuilder {
	private final PrettyPrinter pp = new PrettyPrinter();
	private final JsonFactory jf = new JsonFactory();
	private boolean isPretty;
	private boolean objectComma;
	private boolean openedCurly;
	
	JSBuilder() {
		pp.indentWidth(2);
		pp.setNlAtEnd(false);
	}
	
	public void setPretty(boolean b) {
		isPretty = b;
	}

	public void orb() {
		append("(");
	}
	
	public void crb() {
		append(")");
	}
	
	public void ocb() {
		if (isPretty && !pp.hasWhitespace() && pp.lastChar() != '(') append(" ");
		append("{");
		this.openedCurly = true;
	}
	
	public void ccb() {
		if (!openedCurly && isPretty)
			pp.indentLess();
		this.objectComma = false;
		this.openedCurly = false;
		append("}");
		if (isPretty)
			pp.requireNewline();
	}

	public void osb() {
		append("[");
		if (isPretty)
			pp.indentMore();
	}
	
	public void csb() {
		if (isPretty)
			pp.indentLess();
		append("]");
	}

	public void semi() {
		append(";");
		if (isPretty)
			pp.requireNewline();
	}

	public void objectComma() {
		this.objectComma = true;
	}

	public void assign() {
		if (isPretty) append(" = ");
		else append("=");
	}
	
	public void ident(String s) {
		if (!pp.hasWhitespace() && Character.isJavaIdentifierPart(pp.lastChar()))
			pp.append(' ');
		append(s);
	}

	public void fieldName(String field) {
		if (field == null)
			throw new UtilException("field name cannot be null");
		else
			append(field.toString());
		append(":");
		if (isPretty) append(" ");
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
			else if (value instanceof Integer)
				jg.writeNumber((Integer)value);
			else
				throw new UtilException("JSBuilder cannot write a value of type " + value.getClass());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void append(String s) {
		if (openedCurly) {
			if (isPretty)
				pp.indentMore();
			openedCurly = false;
		}
		if (objectComma) {
			pp.append(",");
			objectComma = false;
			if (isPretty)
				pp.requireNewline();
		}
		pp.append(s);
	}
	
	@Override
	public String toString() {
		return pp.toString();
	}
}
