package com.gmmapowell.xml;

public interface XMLErrorHandler {

	void parseError(XMLParseError ex);

	void missingAttribute(Location from, Location to, String attr);

	void unprocessedAttribute(Location startElt, Location endElt, String attr);

	void invalidTag(Location start, Location end, String tag);

}
