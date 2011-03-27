package com.gmmapowell.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XML {
	Document doc;
	private XMLElement top;
	private final String version;

	private XML(InputStream stream) {
		try
		{
			DocumentBuilder bldr = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = bldr.parse(stream);
			version = "1.0"; // TODO: actually get this from the PI
			top = new XMLElement(this, doc.getDocumentElement());
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	private XML(String version, String tag) {
		this.version = version;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.appendChild(doc.createElement(tag));
			top = new XMLElement(this, doc.getDocumentElement());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static XML fromFile(File f) throws IOException {
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(f);
			XML ret = new XML(fis);
			return ret;
		}
		finally {
			if (fis != null)
				fis.close();
		}
	}

	public static XML fromResource(String name)
	{
		InputStream resource = XML.class.getResourceAsStream(name);
		if (resource == null)
		{
			resource = XML.class.getResourceAsStream("/" + name);
			if (resource == null)
				throw new UtilException("Could not find resource " + name);
		}
		return new XML(resource);
	}

	public XMLElement top() {
		return top;
	}

	public static XML create(String version, String tag) {
		return new XML(version, tag);
	}

	public void write(File file) {
		try
		{
			FileUtils.assertDirectory(file.getParentFile());
			FileOutputStream fos = new FileOutputStream(file);
			OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
			of.setVersion(version);
			of.setIndent(1);
			of.setIndenting(true);
			XMLSerializer serializer = new XMLSerializer(fos, of);
			serializer.asDOMSerializer();
			serializer.serialize(doc);
			fos.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public XMLElement addElement(String tag) {
		return top.addElement(tag);
	}
}
