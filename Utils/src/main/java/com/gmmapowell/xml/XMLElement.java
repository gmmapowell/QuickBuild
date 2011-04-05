package com.gmmapowell.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.gmmapowell.exceptions.UtilException;

public class XMLElement {
	private final XML inside;
	final Element elt;
	private final HashSet<String> attrsProcessed = new HashSet<String>();

	XMLElement(XML inside, Element elt) {
		this.inside = inside;
		this.elt = elt;
	}
	
	public String tag() {
		return elt.getTagName();
	}

	public boolean hasTag(String tag) {
		return tag().equals(tag);
	}

	public void assertTag(String tag) {
		if (!tag.equals(tag()))
			throw new UtilException("The element " + this + " does not have tag " + tag);
	}

	// Linear Access to attributes
	public String required(String attr) {
		if (!elt.hasAttribute(attr))
			throw new UtilException("The required attribute " + attr + " was not found on " + this);
		attrsProcessed.add(attr);
		return elt.getAttribute(attr);
	}
	
	public String optional(String attr) {
		if (elt.hasAttribute(attr))
		{
			attrsProcessed.add(attr);
			return elt.getAttribute(attr);
		}
		return null;
	}
	
	public String optional(String attr, String def) {
		if (elt.hasAttribute(attr))
		{
			attrsProcessed.add(attr);
			return elt.getAttribute(attr);
		}
		return def;
	}
	
	public void attributesDone() {
		if (attrsProcessed.size() != elt.getAttributes().getLength())
		{
			StringBuilder msg = new StringBuilder("At end of attributes processing for " + tag() + ", attributes were unprocessed:");
			for (String a : attributes())
				if (!attrsProcessed.contains(a))
					msg.append(" " + a);
			throw new UtilException(msg.toString());
		}
	}
	
	// Random Access
	public String get(String attr) {
		if (!elt.hasAttribute(attr))
			throw new UtilException("The required attribute " + attr + " was not found on " + this);
		return elt.getAttribute(attr);
	}
	
	// Children functions
	public List<XMLElement> elementChildren() {
		ArrayList<XMLElement> ret = new ArrayList<XMLElement>();
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Element)
				ret.add(new XMLElement(inside, (Element)n));
		}
		return ret;
	}
	

	// The idea of this is to use introspection and reflection to figure out what the object wants,
	// and then give it that in the most appropriate order.
	// I would like this to involve dynamically re-ordering the input file if the alternative would be an error,
	// but that seems difficult to get right in the general case.
	public void populate(Object cxt, Object callbacks) {
		ObjectMetaInfo info = new ObjectMetaInfo(callbacks);
		
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Element)
			{
				XMLElement xe = new XMLElement(inside, (Element)n);
				Object inner = info.dispatch(cxt, xe);
				if (inner == null)
				{
					xe.assertNoSubContents();
					// then there had better be no non-whitespace children of xe
				}
				else if (!(inner instanceof XMLCompletelyHandled))
				{
					xe.populate(cxt, inner);
				}
			}
			else if (n instanceof Text)
			{
				if (!info.wantsText)
					continue;
				if (callbacks instanceof XMLContextTextReceiver)
					((XMLContextTextReceiver)callbacks).receiveText(cxt, ((Text)n).getData());
				else if (callbacks instanceof XMLTextReceiver)
					((XMLTextReceiver)callbacks).receiveText(((Text)n).getData());
				else
					throw new UtilException("There is no valid text handler");
			}
		}
		if (callbacks instanceof XMLNotifyOnComplete)
			((XMLNotifyOnComplete)callbacks).complete();
	}

	public void assertNoSubContents() {
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Comment)
				continue;
			if (n instanceof Text)
			{
				String s = ((Text)n).getData();
				for (char c : s.toCharArray())
				{
					if (!Character.isWhitespace(c))
						throw new UtilException(this + " cannot have non-whitespace children");
				}
				continue;
			}
			throw new UtilException("This node cannot have " + n.getClass() + " as a child"); 
		}
	}

	@Override
	public String toString() {
		return "Element " + elt.getTagName();
	}

	public String text() {
		return elt.getTextContent();
	}

	public List<String> attributes() {
		List<String> ret = new ArrayList<String>();
		NamedNodeMap attributes = elt.getAttributes();
		for (int i=0;i<attributes.getLength();i++)
			ret.add(((Attr)attributes.item(i)).getName());
		return ret;
	}

	public XMLElement addElement(String tag) {
		Element child = inside.doc.createElement(tag);
		elt.appendChild(child);
		return new XMLElement(inside, child);
	}

	public void setAttribute(String attr, String value) {
		elt.setAttribute(attr, value);
	}

	public boolean hasAttribute(String attr) {
		return elt.hasAttribute(attr);
	}
}
