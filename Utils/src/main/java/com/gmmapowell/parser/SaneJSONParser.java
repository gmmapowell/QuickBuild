package com.gmmapowell.parser;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.gmmapowell.exceptions.UtilException;

public class SaneJSONParser {
	private JsonParser jp;
	private JsonToken ctok;
	private String text;

	// TODO: should also support reader, etc...
	public SaneJSONParser(String input) {
		try {
			JsonFactory jf = new JsonFactory();
			jp = jf.createJsonParser(input);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
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

	public boolean is(JsonToken possible) {
		boolean ret = possible == get();
		if (ret)
			accept();
		return ret;
	}

	public boolean is(JsonToken possible, String withText) {
		boolean ret = possible == get();
		if (!ret)
			return false;
		if (!withText.equals(text))
			return false;
		accept();
		return true;
	}


	@SuppressWarnings("unchecked")
	public <T> T extractField() {
		JsonToken tok;
		tok = get();
		T ret;
		if (tok == JsonToken.VALUE_STRING) {
			ret = (T)text;
		} else
			throw new UtilException("That is not handled");
		accept();
		return ret;
	}

	public <T> T extractField(String field) {
		JsonToken tok = get();
		if (tok != JsonToken.FIELD_NAME)
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
