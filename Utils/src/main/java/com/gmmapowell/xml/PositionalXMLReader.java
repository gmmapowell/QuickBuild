package com.gmmapowell.xml;
// PositionalXMLReader.java

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PositionalXMLReader {

    public static Document readXML(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
//            factory.setFeature("http://xml.org/sax/features/declaration-handler", true);
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final DefaultHandler handler = new LocationAnnotator(doc);
//        parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
//        parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
//        parser.getXMLReader().setContentHandler(handler);
//        parser.getXMLReader().setErrorHandler(handler);
//        parser.getXMLReader().setFeature("http://xml.org/sax/features/lexical-handler/parameter-entities", true);
        parser.parse(is, handler);

        return doc;
    }
}