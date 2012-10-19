package com.gmmapowell.extrep;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.java.xml.stream.XMLStreamWriter;

public class XMLERObject extends ERObject {
	private XMLStreamWriter gen;

	public XMLERObject(OutputStream out, String tag) {
		super(tag);
		try {
			WstxOutputFactory xf = new WstxOutputFactory();
//			gen = new IndentingXMLStreamWriter(xf.createXMLStreamWriter(out));
			gen = xf.createXMLStreamWriter(out);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public XMLERObject(Writer out, String tag) {
		super(tag);
		try {
			WstxOutputFactory xf = new WstxOutputFactory();
//			gen = new IndentingXMLStreamWriter(xf.createXMLStreamWriter(out));
			gen = xf.createXMLStreamWriter(out);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	XMLERObject(XMLStreamWriter gen, String tag) {
		super(tag);
		this.gen = gen;
	}

	@Override
	public void done() {
		try {
			gen.writeStartElement(tag);
			for (Map.Entry<String,Object> kv : attrs.entrySet()) {
				gen.writeAttribute(kv.getKey(), (String) kv.getValue());
			}
			for (Map.Entry<String,Object> kv : elements.entrySet()) {
				Object value = kv.getValue();
				if (value instanceof XMLERObject)
					((XMLERObject)value).done();
				else if (value instanceof XMLERList)
					((XMLERList)value).done();
				else
					throw new UtilException("Cannot handle " + value.getClass());
			}
			if (soup != null)
				((XMLERSoup)soup).done();
			gen.writeEndElement();
			gen.flush();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	protected ERObject newObject(String tag) {
		return new XMLERObject(gen, tag);
	}

	@Override
	protected ERList newList() {
		return new XMLERList(gen);
	}

	@Override
	protected ERSoup newSoup() {
		return new XMLERSoup(gen);
	}
}
