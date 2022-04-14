package com.cubrid.parser;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import com.cubrid.analyzer.SQLAnalyzerForCUBRID;
import com.cubrid.database.DatabaseManager;

public class SqlMapXMLParser {
	private SAXParserFactory saxParserFactory = null;
	private SAXParser saxParser = null;
	private XMLReader xmlReader = null;

	public SqlMapXMLParser() {
		saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);
		saxParserFactory.setValidating(false);

		try {
			saxParser = saxParserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		try {
			xmlReader = saxParser.getXMLReader();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public void analyze(SQLAnalyzerForCUBRID sqlAnalyzer, DatabaseManager databaseManager, String filePath,
			String fileName) {
		SqlXmlHandler sqlXmlHandler = new SqlXmlHandler(sqlAnalyzer, databaseManager, filePath, fileName);

		try {
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", sqlXmlHandler);
		} catch (SAXNotRecognizedException e) {
			e.printStackTrace();
		} catch (SAXNotSupportedException e) {
			e.printStackTrace();
		}

		try {
			saxParser.parse(filePath, sqlXmlHandler);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
