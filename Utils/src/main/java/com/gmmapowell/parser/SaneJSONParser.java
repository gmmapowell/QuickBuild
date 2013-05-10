package com.gmmapowell.parser;

import java.io.Reader;
import java.util.List;

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
	
	/** Check if the current token matches the possible token, but do NOT accept it
	 * @param possible the token to compare to
	 * @return true if the current token matches the sample; false otherwise
	 */
	public boolean is(JsonToken possible) {
		return possible == get();
	}

	/** Check if the current token matches the possible token, and accept it if true
	 * @param possible the token to compare to
	 * @return true if the current token matches the sample; false otherwise
	 */
	public boolean match(JsonToken possible) {
		boolean ret = possible == get();
		if (ret)
			accept();
		return ret;
	}

	/** Check if the current token matches the possible token, and accept it if true.
	 * Only consider it a match if the current text node also matches the specified text.
	 * 
	 * @param possible the token to compare to
	 * @return true if the current token matches the sample; false otherwise
	 */
	public boolean match(JsonToken possible, String withText) {
		boolean ret = possible == get();
		if (!ret)
			return false;
		if (!withText.equals(text))
			return false;
		accept();
		return true;
	}

	/** Check that the current token is a field name and return the name of the field
	 * 
	 * @return the name of the next field
	 */
	public String getFieldName() {
		JsonToken tok = get();
		if (tok != JsonToken.FIELD_NAME)
			throw new UtilException("Expected FIELD_NAME but was '" + tok + "' (" + text + ")");
		String ret = text;
		accept();
		return ret;
	}

	@SuppressWarnings("unchecked")
	/** Extract the value of the current token/field
	 * 
	 * @return the converted value of the current token's text.
	 */
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
		else if (tok == JsonToken.VALUE_NULL)
			ret = null;
		else
			throw new UtilException("That is not handled: " + tok);
		accept();
		return ret;
	}

	/** Extract the value of the next field
	 * 
	 * @param field the name of the current field, which must match
	 * @return the value of the named field
	 */
	public <T> T extractField(String field) {
		JsonToken tok = get();
		if (tok != JsonToken.FIELD_NAME || !text.equals(field))
			throw new UtilException("Expected FIELD_NAME (" + field + ") but was '" + tok + "' (" + text + ")");
		accept();
		return extractField();
	}

	/** Read a list of strings from the input (as an array) and store in the provided list
	 * 
	 * @param into a list of strings to append to
	 */
	public void readStringList(List<String> into) {
		match(JsonToken.START_ARRAY);
		while (!is(JsonToken.END_ARRAY)) {
			into.add((String)extractField());
		}
		match(JsonToken.END_ARRAY);
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

	public String currentTok() {
		return ctok.toString() + "[" + text + "]";
	}
}
