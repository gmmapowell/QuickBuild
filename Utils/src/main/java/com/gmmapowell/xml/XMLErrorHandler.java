package com.gmmapowell.xml;

public interface XMLErrorHandler {

	void missingAttribute(String attr);

	void unprocessedAttribute(String attr);
}
