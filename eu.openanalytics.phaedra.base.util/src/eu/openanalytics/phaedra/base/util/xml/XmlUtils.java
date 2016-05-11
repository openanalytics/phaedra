package eu.openanalytics.phaedra.base.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlUtils {

	private static DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilderFactory xmlFactoryNS = DocumentBuilderFactory.newInstance();
	static {
		xmlFactoryNS.setNamespaceAware(true);
	}
	
	private static XPath searcher = XPathFactory.newInstance().newXPath();

	public static Document parse(InputStream in) throws IOException {
		try {
			DocumentBuilder db = xmlFactory.newDocumentBuilder();
			Document doc = db.parse(in);
			return doc;
		} catch (ParserConfigurationException e) {
			throw new IOException("Failed to set up XML parser", e);
		} catch (SAXException e) {
			throw new IOException("Failed to parse XML", e);
		}
	}

	public static Document parse(String xml) throws IOException {
		return parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
	}

	public static Document createEmptyDoc() throws IOException {
		return createEmptyDoc(false);
	}

	public static Document createEmptyDoc(boolean namespaceAware) throws IOException {
		try {
			DocumentBuilder db = null;
			if (namespaceAware) db = xmlFactoryNS.newDocumentBuilder();
			else db = xmlFactory.newDocumentBuilder();
			return db.newDocument();
		} catch (ParserConfigurationException e) {
			throw new IOException("Failed to set up XML parser", e);
		}
	}
	
	public static Element createTag(Document doc, Node parent, String name) {
		return (Element)parent.appendChild(doc.createElement(name));
	}
	
	public static Element createTag(Document doc, Node parent, String localName, String prefix, String ns) {
		String nodeName = (prefix == null) ? localName : prefix+":"+localName;
		Element el = (Element)parent.appendChild(doc.createElement(nodeName));
		if (ns != null) {
			String att = (prefix == null) ? "xmlns" : "xmlns:"+prefix;
			el.setAttribute(att, ns);
		}
		return el;
	}
	
	public static String writeToString(Document doc) throws IOException {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result);
			return sw.toString();
		} catch (TransformerException e) {
			throw new IOException("XML to String failed", e);
		}
	}
	
	public static void setNamespaceContext(NamespaceContext ctx) {
		searcher.setNamespaceContext(ctx);
	}
	
	/**
	 * Look up a Node in an XML tree using its name.
	 * 
	 * @param element
	 *            The root to start looking in
	 * @param name
	 *            The name of the Node to look for
	 * @return The first matching Node
	 */
	public static Node getNodeByName(Element element, String name) {
		if (element == null)
			return null;
		NodeList nodeList = element.getElementsByTagName(name);
		if (nodeList.getLength() > 0) {
			return nodeList.item(0);
		}
		return null;
	}

	public static Element getElementByName(Element element, String name) {
		if (element == null)
			return null;

		NodeList nodeList = element.getElementsByTagName(name);
		if (nodeList.getLength() > 0) {
			return (Element) nodeList.item(0);
		}
		return null;
	}

	/**
	 * Get a named attribute of a node, or null if it doesn't exist.
	 * 
	 * @param node
	 *            The node that contains the attribute.
	 * @param attrName
	 *            The name of the attribute.
	 * @return The attribute value as a String, or null if there is none.
	 */
	public static String getNodeAttr(Node node, String attrName) {
		NamedNodeMap attributes = node.getAttributes();
		String value = null;
		if (attributes != null) {
			Node attribute = attributes.getNamedItem(attrName);
			if (attribute != null)
				value = attribute.getTextContent();
		}
		return value;
	}

	/**
	 * Get the value of a node.
	 * 
	 * @param node
	 *            The node whose value must be returned.
	 * @return The value of the node. If the node has subnodes, the result of
	 *         this method is unspecified.
	 */
	public static String getNodeValue(Node node) {
		Node child = node.getChildNodes().item(0);
		if (child == null)
			return null;
		return child.getNodeValue();
	}

	/*
	 * XPATH Utility methods ---------------------
	 */

	/**
	 * Find a String value (i.e. a tag or attribute value) using the specified
	 * XPATH query.
	 * 
	 * @param query
	 *            The XPath query.
	 * @return The text value, or null if the XPath did not match.
	 */
	public static String findString(String query, Document doc) {
		try {
			return searcher.evaluate(query, doc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Failed to evaluate XPath: " + query, e);
		}
	}

	/**
	 * Find an integer value (i.e. a tag or attribute value) using the specified
	 * XPATH query.
	 * 
	 * @param query
	 *            The XPath query.
	 * @return The integer value, null if not found.
	 */
	public static Integer findInt(String query, Document doc) {
		String val = findString(query, doc);
		if (val != null)
			try {
				return Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		return null;
	}
	
	/**
	 * Find a double value (i.e. a tag or attribute value) using the specified
	 * XPATH query.
	 * 
	 * @param query
	 *            The XPath query.
	 * @return The double value, null if not found.
	 */
	public static Double findDouble(String query, Document doc) {
		String val = findString(query, doc);
		if (val != null)
			try {
				return Double.parseDouble(val);
			} catch (NumberFormatException e) {
			}
		return null;
	}

	/**
	 * Find all tags that match the specified XPATH query.
	 * 
	 * @param query
	 *            The XPath query.
	 * @return The matching tags, possibly null.
	 */
	public static NodeList findTags(String query, Document doc) {
		try {
			return (NodeList) searcher.evaluate(query, doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Failed to evaluate XPath: " + query, e);
		}
	}
	
	public static NodeList findTags(String query, Node node) {
		try {
			return (NodeList) searcher.evaluate(query, node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Failed to evaluate XPath: " + query, e);
		}
	}
	
	public static Element getFirstElement(String query, Document doc) {
		NodeList nodes = findTags(query, doc);
		if (nodes.getLength() == 0) return null;
		return (Element)nodes.item(0);
	}
	
	public static Element getFirstElement(String query, Node node) {
		NodeList nodes = findTags(query, node);
		if (nodes.getLength() == 0) return null;
		return (Element)nodes.item(0);
	}

	/*
	 * Other utility methods ---------------------
	 */

	/**
	 * @deprecated Does not work correctly for multiple primary keys.
	 */
	public static String convertIdsToXml(List<Integer> ids, String pojo) {
		StringBuilder sb = new StringBuilder();

		sb.append("<keys pojo='");
		sb.append(pojo);
		sb.append("'>");

		for (Integer id : ids) {
			sb.append("<id>");
			sb.append(id);
			sb.append("</id>");
		}

		sb.append("</keys>");

		return sb.toString();
	}

	public static double getDoubleAttribute(NamedNodeMap attributes, String name) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return Double.NaN;
		String value = attribute.getNodeValue();
		
		Double val = Double.NaN;
		
		try {
			val = Double.parseDouble(value);
		} catch (NumberFormatException e) {
			//take default (Double.NaN)
		}
		
		return val;
	}

	public static int getIntegerAttribute(Element tag, String name) {
		return getIntegerAttribute(tag, name, 0);
	}
	
	public static int getIntegerAttribute(Element tag, String name, int defaultValue) {
		String value = tag.getAttribute(name);
		if (value != null && !value.isEmpty()) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}
	
	public static int getIntegerAttribute(NamedNodeMap attributes, String name) {
		return getIntegerAttribute(attributes, name, 0);
	}

	public static int getIntegerAttribute(NamedNodeMap attributes, String name,
			int defaultValue) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return defaultValue;
		String value = attribute.getNodeValue();
		return Integer.parseInt(value);
	}

	public static long getLongAttribute(NamedNodeMap attributes, String name) {
		return getLongAttribute(attributes, name, 0);
	}

	public static long getLongAttribute(NamedNodeMap attributes, String name,
			long defaultValue) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return defaultValue;
		String value = attribute.getNodeValue();
		return Long.parseLong(value);
	}

	public static String getStringAttribute(NamedNodeMap attributes, String name) {
		return getStringAttribute(attributes, name, "");
	}

	public static String getStringAttribute(NamedNodeMap attributes,
			String name, String defaultValue) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return defaultValue;
		return attribute.getNodeValue();
	}

	public static String makeStringAttribute(String name, String value) {

		if (value == null) {
			return "";
		}

		String checkedValue = value.replaceAll("&", "&amp;");
		checkedValue = checkedValue.replaceAll("<", "&lt;");
		checkedValue = checkedValue.replaceAll(">", "&gt;");
		checkedValue = checkedValue.replaceAll("\"", "&quot;");
		checkedValue = checkedValue.replaceAll("'", "&apos;");

		String attribute = name + "= \"";
		attribute += checkedValue;
		attribute += "\" ";
		return attribute;
	}

	public static String makeDoubleAttribute(String name, double value) {
		String attribute = name + "= \"";
		if (value != Double.NaN)
			attribute += String.valueOf(value);
		attribute += "\" ";
		return attribute;
	}

	public static String makeIntAttribute(String name, int value) {
		String attribute = name + "= \"";
		attribute += String.valueOf(value);
		attribute += "\" ";
		return attribute;
	}

	public static String makeLongAttribute(String name, long value) {
		String attribute = name + "= \"";
		attribute += String.valueOf(value);
		attribute += "\" ";
		return attribute;
	}

	public static Date getDateAttribute(NamedNodeMap attributes, String name) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return null;
		String value = attribute.getNodeValue();
		if (value == null)
			return null;
		if (value.length() == 0)
			return null;
		Date date = new Date(Long.parseLong(value));

		return date;
	}

	public static String makeDateAttribute(String name, Date value) {
		String attribute = name + "= \"";
		if (value != null)
			attribute += value.getTime();
		attribute += "\" ";
		return attribute;
	}

	public static boolean getBooleanAttribute(NamedNodeMap attributes,
			String name) {
		return getBooleanAttribute(attributes, name, false);
	}

	public static boolean getBooleanAttribute(NamedNodeMap attributes,
			String name, boolean defaultValue) {
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null)
			return defaultValue;
		String value = attribute.getNodeValue();
		if (value == null)
			return defaultValue;

		return value.equalsIgnoreCase("true");
	}

	public static String makeBooleanAttribute(String name, boolean value) {
		String attribute = name + "= \"";
		attribute += value;
		attribute += "\" ";
		return attribute;
	}

}
