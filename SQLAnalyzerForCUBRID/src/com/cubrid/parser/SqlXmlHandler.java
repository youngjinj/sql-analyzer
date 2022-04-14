package com.cubrid.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;

import com.cubrid.analyzer.SQLAnalyzerForCUBRID;
import com.cubrid.database.DatabaseManager;

public class SqlXmlHandler extends DefaultHandler2 implements LexicalHandler {
	private final String NO_ERROR = "NO_ERROR";

	private final String TAG_SQLMAP = "SQLMAP";
	private final String TAG_MAPPER = "MAPPER";

	private final String TAG_SELECT = "SELECT";
	private final String TAG_INSERT = "INSERT";
	private final String TAG_UPDATE = "UPDATE";
	private final String TAG_DELETE = "DELETE";

	private final String TAG_DML = TAG_SELECT + "|" + TAG_INSERT + "|" + TAG_UPDATE + "|" + TAG_DELETE;

	private final String TAG_SELECTKEY = "SELECTKEY";
	private final String TAG_SET = "SET";
	private final String TAG_DYNAMIC = "DYNAMIC";
	private final String TAG_ITERATE = "ITERATE";
	private final String TAG_ISNOTEMPTY = "ISNOTEMPTY";
	private final String TAG_ISEMPTY = "ISEMPTY";
	private final String TAG_ISNOTEQUAL = "ISNOTEQUAL";
	private final String TAG_ISEQUAL = "ISEQUAL";
	private final String TAG_ISGREATEREQUAL = "ISGREATEREQUAL";
	private final String TAG_ISLESSTHAN = "ISLESSTHAN";

	private final String TAG_COMMAND = TAG_SELECTKEY + "|" + TAG_SET + "|" + TAG_DYNAMIC + "|" + TAG_ITERATE + "|"
			+ TAG_ISNOTEMPTY + "|" + TAG_ISEMPTY + "|" + TAG_ISNOTEQUAL + "|" + TAG_ISEQUAL + "|" + TAG_ISGREATEREQUAL
			+ "|" + TAG_ISLESSTHAN;

	private final String TAG_RESULT = "RESULT";
	private final String TAG_RESULTMAP = "RESULTMAP";
	private final String TAG_PROPERTIES = "PROPERTIES";
	private final String TAG_COMMENT = "COMMENT";
	private final String TAG_ENTRY = "ENTRY";

	private final String TAG_OTHER = TAG_RESULT + "|" + TAG_RESULTMAP + "|" + TAG_PROPERTIES + "|" + TAG_COMMENT + "|"
			+ TAG_ENTRY;

	private final String TAG_KNOWN = TAG_MAPPER + "|" + TAG_DML + "|" + TAG_COMMAND + "|" + TAG_OTHER;

	private final String PTTRN_MASK1 = "1"; /* '$ ~ $' */
	private final String PTTRN_MASK2 = "'1'"; /* $# ~ $# */

	private HashMap<String, String> mapIsEmpty = null;
	private HashMap<String, String> mapIsEqual = null;
	private HashMap<String, String> mapIsGreaterLess = null;

	private Stack<SqlMapXMLTag> stackReadTag = null;

	private int count = 0;
	private int errorCount = 0;

	private DatabaseManager databaseManager = null;

	private SQLAnalyzerForCUBRID sqlAnalyzer = null;

	private String filePath = null;
	private String fileName = null;
	private String logFilePath = null;

	private BufferedWriter writerLog = null;

	public SqlXmlHandler(SQLAnalyzerForCUBRID sqlAnalyzer, DatabaseManager databaseManager, String filePath, String fileName) {
		this.sqlAnalyzer = sqlAnalyzer;
		this.databaseManager = databaseManager;
		this.filePath = filePath;
		this.fileName = fileName;

		logFilePath = this.filePath.substring(0, filePath.lastIndexOf(".")) + ".log";
		
		try {
			writerLog = new BufferedWriter(new FileWriter(logFilePath));
		} catch (IOException e) {
			e.printStackTrace();
		}

		mapIsEmpty = new HashMap<String, String>();
		mapIsEqual = new HashMap<String, String>();
		mapIsGreaterLess = new HashMap<String, String>();

		stackReadTag = new Stack<SqlMapXMLTag>();

		count = 0;
		errorCount = 0;
	}

	private void openLog() {
		try {
			writerLog.write("/* Open File: " + fileName + " */");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void appendQueryId(String query, String queryId) {
		count++;

		try {
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("================================================================================");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("Number   : " + count);
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("Query    : " + query);
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("Query Id : " + queryId);
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("================================================================================");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void appendQuery(String query) {
		try {
			writerLog.append(query + System.getProperty("line.separator"));
			writerLog.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void appendResult(String error) {
		try {
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("--------------------------------------------------------------------------------");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append(error);
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("--------------------------------------------------------------------------------");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeLog() {
		try {
			writerLog.append(System.getProperty("line.separator"));
			writerLog.append("/* Close File: " + fileName + " */");
			writerLog.append(System.getProperty("line.separator"));
			writerLog.flush();
			writerLog.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startDocument() throws SAXException {
		openLog();
	}

	@Override
	public void endDocument() throws SAXException {
		closeLog();

		sqlAnalyzer.appendResultSummary(count, count - errorCount, errorCount);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		SqlMapXMLTag currentTag = new SqlMapXMLTag();
		saveStartTag(currentTag, localName, attributes);

		if (currentTag.getName().toUpperCase().matches(TAG_DML)) {
			appendQueryId(currentTag.getName(), currentTag.getId());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String contents = new String(ch, start, length);
		contents = contents.replaceAll("\t", "    ");

		String checkEmpty = contents.trim().replaceAll("\\s", "");

		if (!("".equals(checkEmpty))) {
			SqlMapXMLTag currentTag = stackReadTag.peek();
			currentTag.addContents(contents);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		SqlMapXMLTag currentTag = stackReadTag.pop();
		SqlMapXMLTag beforeTag = null;

		if (stackReadTag.size() > 0) {
			beforeTag = stackReadTag.peek();
		}

		switch (currentTag.getName().toUpperCase()) {
		case TAG_SELECT:
		case TAG_INSERT:
		case TAG_UPDATE:
		case TAG_DELETE:
			String query = pttrnMtchSQL(currentTag.getContents()) + ";";
			String result = null;
			appendQuery(query);
			result = databaseManager.prepareQuery(query);
			if (!NO_ERROR.equals(result)) {
				errorCount++;
			}
			appendResult(result);
			break;

		default:
			break;
		}

		if (!("".equals(currentTag.getContents().trim()))) {
			switch (currentTag.getName().toUpperCase()) {
			case TAG_SELECTKEY:
				appendQuery(new String(pttrnMtchSQL(currentTag.getContents()) + ";"));
				break;

			case TAG_SET:
				if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)
						&& (currentTag.getClose() == null)) {
					beforeTag.addContents(" SET " + currentTag.getContents() + " ");
				}
				break;

			case TAG_DYNAMIC:
				if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)
						&& (currentTag.getClose() == null)) {
					beforeTag.addContents(" " + currentTag.getContents() + " ");
				}
				if ((currentTag.getPrepend() != null) && (currentTag.getOpen() == null)
						&& (currentTag.getClose() == null)) {
					beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
				}
				if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)
						&& (currentTag.getClose() != null)) {
					beforeTag.addContents(" " + currentTag.getPrepend() + currentTag.getOpen()
							+ currentTag.getContents() + currentTag.getClose() + " ");
				}
				break;

			case TAG_ITERATE:
				if ((currentTag.getPrepend() == null) && (currentTag.getOpen() != null)
						&& (currentTag.getClose() != null) && (currentTag.getConjunction() != null)) {
					beforeTag.addContents(pttrnMtchIterate(currentTag.getContents(),
							" " + currentTag.getOpen() + PTTRN_MASK2 + currentTag.getConjunction() + PTTRN_MASK2
									+ currentTag.getConjunction() + PTTRN_MASK2 + currentTag.getClose() + " "));
				}
				if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)
						&& (currentTag.getClose() != null) && (currentTag.getConjunction() != null)) {
					beforeTag.addContents(currentTag.getPrepend() + pttrnMtchIterate(currentTag.getContents(),
							" " + currentTag.getOpen() + PTTRN_MASK2 + currentTag.getConjunction() + PTTRN_MASK2
									+ currentTag.getConjunction() + PTTRN_MASK2 + currentTag.getClose() + " "));
				}
				break;

			case TAG_ISNOTEMPTY:
				if (!(mapIsEmpty.containsKey(currentTag.getProperty()))) {
					mapIsEmpty.put(currentTag.getProperty(), "NOTEMPTY");
				}

				if ("NOTEMPTY".equals(mapIsEmpty.get(currentTag.getProperty()).toUpperCase())) {
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() != null)) {
						beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() == null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getContents() + " ");
						}
						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
						}
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
						}

						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getOpen()
									+ currentTag.getContents() + " ");
						}
					}
				}
				break;

			case TAG_ISEMPTY:
				if (!(mapIsEmpty.containsKey(currentTag.getProperty()))) {
					mapIsEmpty.put(currentTag.getProperty(), "NOTEMPTY");
				}
				if ("EMPTY".equals(mapIsEmpty.get(currentTag.getProperty()).toUpperCase())) {
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() != null)) {
						beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() == null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getContents() + " ");
						}

						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
						}
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
						}

						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getOpen()
									+ currentTag.getContents() + " ");
						}
					}
				}
				break;

			case TAG_ISNOTEQUAL:
				if (!(mapIsEqual.containsKey(currentTag.getProperty()))) {
					mapIsEqual.put(currentTag.getProperty(), currentTag.getCompareValue());
				}
				if (!(currentTag.getCompareValue().equals(mapIsEqual.get(currentTag.getProperty())))) {
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() != null)) {
						beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() == null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getContents() + " ");
						}
						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
						}
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
						}
						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getOpen()
									+ currentTag.getContents() + " ");
						}
					}
				}
				break;

			case TAG_ISEQUAL:
				if (!(mapIsEqual.containsKey(currentTag.getProperty()))) {
					mapIsEqual.put(currentTag.getProperty(), currentTag.getCompareValue());
				}
				if (currentTag.getCompareValue().equals(mapIsEqual.get(currentTag.getProperty()))) {
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() == null)) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() == null) && (currentTag.getOpen() != null)) {
						beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() == null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getContents() + " ");
						}

						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
						}
					}
					if ((currentTag.getPrepend() != null) && (currentTag.getOpen() != null)) {
						if ((beforeTag.getPrepend() != null) && (beforeTag.getContents().length() == 0)
								&& (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getOpen() + currentTag.getContents() + " ");
						}

						if ((beforeTag.getContents().length() > 0) && (currentTag.getContents().length() > 0)) {
							beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getOpen()
									+ currentTag.getContents() + " ");
						}
					}
				}
				break;

			case TAG_ISGREATEREQUAL:
				if (!(mapIsGreaterLess.containsKey(currentTag.getProperty()))) {
					mapIsGreaterLess.put(currentTag.getProperty(),
							String.valueOf(Integer.parseInt(currentTag.getCompareValue()) + 1));
				}
				if (Integer.parseInt(mapIsGreaterLess.get(currentTag.getProperty())) >= Integer
						.parseInt(currentTag.getCompareValue())) {
					if (currentTag.getPrepend() == null) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if (currentTag.getPrepend() != null) {
						beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
					}
				}
				break;

			case TAG_ISLESSTHAN:
				if (!(mapIsGreaterLess.containsKey(currentTag.getProperty()))) {
					mapIsGreaterLess.put(currentTag.getProperty(),
							String.valueOf(Integer.parseInt(currentTag.getCompareValue()) + 1));
				}
				if (Integer.parseInt(mapIsGreaterLess.get(currentTag.getProperty())) < Integer
						.parseInt(currentTag.getCompareValue())) {
					if (currentTag.getPrepend() == null) {
						beforeTag.addContents(" " + currentTag.getContents() + " ");
					}
					if (currentTag.getPrepend() != null) {
						beforeTag.addContents(" " + currentTag.getPrepend() + " " + currentTag.getContents() + " ");
					}
				}
				break;

			default:
				break;
			}
		}
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	@Override
	public void endDTD() throws SAXException {
	}

	@Override
	public void startEntity(String name) throws SAXException {
	}

	@Override
	public void endEntity(String name) throws SAXException {
	}

	@Override
	public void startCDATA() throws SAXException {
	}

	@Override
	public void endCDATA() throws SAXException {
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
	}
	
	private void saveStartTag(SqlMapXMLTag tag, String localName, Attributes attributes) {
		tag.setName(localName);

		if (attributes.getValue("id") != null) {
			tag.setId(attributes.getValue("id"));
		}

		if (attributes.getValue("property") != null) {
			tag.setProperty(attributes.getValue("property"));
		}

		if (attributes.getValue("prepend") != null) {
			tag.setPrepend(attributes.getValue("prepend"));
		}

		if (attributes.getValue("compareValue") != null) {
			tag.setCompareValue(attributes.getValue("compareValue"));
		}

		if (attributes.getValue("open") != null) {
			tag.setOpen(attributes.getValue("open"));
		}

		if (attributes.getValue("close") != null) {
			tag.setClose(attributes.getValue("close"));
		}

		if (attributes.getValue("conjunction") != null) {
			tag.setConjunction(attributes.getValue("conjunction"));
		}

		stackReadTag.push(tag);
	}
	
	public String pttrnMtchBlank(Object sql) {
		String pttrn_str1 = "[) ]+\\p{Alnum}+$";

		Pattern pattern = null;

		String query = String.valueOf(sql);

		pattern = Pattern.compile(pttrn_str1);
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {
			return query + " ";
		}

		return query;
	}

	public String pttrnMtchSQL(Object sql) {
		String pttrn_str1 = "[$][^$]+[$]";
		String pttrn_str2 = "[#][^#]+[#]"; /* mybatis */
		String pttrn_str3 = "[#{][^#]+[}]";

		Pattern pattern = null;

		String query = String.valueOf(sql);

		pattern = Pattern.compile(pttrn_str1);
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {
			query = matcher.replaceAll("?");
		}

		pattern = Pattern.compile(pttrn_str2);
		matcher = pattern.matcher(query);
		while (matcher.find()) {
			query = matcher.replaceAll("?");
		}

		pattern = Pattern.compile(pttrn_str3);
		matcher = pattern.matcher(query);
		while (matcher.find()) {
			query = matcher.replaceAll("?");
		}
		
		return query;
	}

	public String pttrnMtchIterate(String replaceStr, String insertStr) {
		String pttrn_str1 = "[#][^#]+[#]";

		Pattern pattern = null;

		pattern = Pattern.compile(pttrn_str1);
		Matcher matcher = pattern.matcher(replaceStr);
		while (matcher.find()) {
			replaceStr = matcher.replaceAll(insertStr);
		}

		return replaceStr;
	}
}