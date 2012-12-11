package com.gmmapowell.xml;

import com.gmmapowell.exceptions.InvalidXMLTagException;
import com.gmmapowell.exceptions.XMLMissingAttributeException;
import com.gmmapowell.exceptions.XMLUnprocessedAttributeException;

public interface XMLErrorHandler {

	void parseError(XMLParseError ex);

	void missingAttribute(Location from, Location to, XMLMissingAttributeException ex);

	void unprocessedAttribute(Location from, Location to, XMLUnprocessedAttributeException ex);

	void invalidTag(Location start, Location end, InvalidXMLTagException ex);

}
