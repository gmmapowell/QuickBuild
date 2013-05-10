package com.gmmapowell.parser;

import java.io.Reader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.gmmapowell.exceptions.UtilException;

public class SaneJSONParser {
	private JsonParser jp;
	private JsonToken ctok;
	private String text;

	public SaneJSONParser(String input) {
		try {
			JsonFactory jf = new JsonFactory();
			jp = jf.createJsonParser(input);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public SaneJSONParser(Reader input) {
		try {
			JsonFactory jf = new JsonFactory();
			jp = jf.createJsonParser(input);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public JsonParser getParser() {
		if (ctok != null)
			throw new UtilException("Cannot extract parser while in lookahead mode");
		return jp;
	}
	
	void accept() {
		ctok = null;
		text = null;
	}

	public void check(JsonToken expected) {
		JsonToken tok = get();
		if (tok != expected)
			throw new UtilException("Expected token '" + expected + "' but was '" + tok + "' (" + text + ")");
		accept();
	}

	public void check(JsonToken expected, String withText) {
		JsonToken tok = get();
		if (tok != expected)
			throw new UtilException("Expected token '" + expected + "' but was '" + tok + "' (" + text + ")");
		if (!withText.equals(text))
			throw new UtilException("Expected token text '" + withText + "' but was '" + text + "'");
		accept();
	}
	
	public boolean is(JsonToken possible) {
		return possible == get();
	}

	public boolean match(JsonToken possible) {
		boolean ret = possible == get();
		if (ret)
			accept();
		return ret;
	}

	public boolean match(JsonToken possible, String withText) {
		boolean ret = possible == get();
		if (!ret)
			return false;
		if (!withText.equals(text))
			return false;
		accept();
		return true;
	}

	public String getFieldName() {
		JsonToken tok = get();
		if (tok != JsonToken.FIELD_NAME)
			throw new UtilException("Expected FIELD_NAME but was '" + tok + "' (" + text + ")");
		String ret = text;
		accept();
		return ret;
	}

	@SuppressWarnings("unchecked")
	public <T> T extractField() {
		JsonToken tok;
		tok = get();
		T ret;
		if (tok == JsonToken.VALUE_STRING)
			ret = (T)text;
		else if (tok == JsonToken.VALUE_NUMBER_INT)
			ret = (T)Integer.valueOf(text);
		else if (tok == JsonToken.VALUE_NUMBER_FLOAT)
			ret = (T)Double.valueOf(text);
		else if (tok == JsonToken.VALUE_TRUE)
			ret = (T)(Boolean)true;
		else if (tok == JsonToken.VALUE_FALSE)
			ret = (T)(Boolean)false;
		else
			throw new UtilException("That is not handled: " + tok);
		accept();
		return ret;
	}

	public <T> T extractField(String field) {
		JsonToken tok = get();
		if (tok != JsonToken.FIELD_NAME || !text.equals(field))
			throw new UtilException("Expected FIELD_NAME (" + field + ") but was '" + tok + "' (" + text + ")");
		accept();
		return extractField();
	}

	private JsonToken get() {
		try {
			if (ctok == null) {
				ctok = jp.nextToken();
				text = jp.getText();
			}
			return ctok;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void atEnd() {
		if (get() != null)
			throw new UtilException("Expected end-of-text");
		close();
	}

	public void close() {
		// Close any open readers, etc ...
	}
}
