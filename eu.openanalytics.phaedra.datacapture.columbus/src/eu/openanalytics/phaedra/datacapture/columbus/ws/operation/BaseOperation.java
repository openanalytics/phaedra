package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public abstract class BaseOperation {
	
	private final static String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
	private final static String COLUMBUS_NS = "http://webservice.columbus.pki.com/xsd";
	
	public abstract void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException;
	
	protected static Element createFunctionTag(Document doc, String tagName) {
		Element envelope = XmlUtils.createTag(doc, doc, "Envelope", "S", SOAP_NS);
		Element body = XmlUtils.createTag(doc, envelope, "S:Body");
		Element tag = XmlUtils.createTag(doc, body, tagName, null, COLUMBUS_NS);
		return tag;
	}
	
	protected static void addKeyValueTags(Document doc, Element parent, String... keyValues) {
		for (int i=0; i<keyValues.length/2; i++) {
			XmlUtils.createTag(doc, parent, keyValues[i*2]).setTextContent(keyValues[i*2+1]);
		}
	}
	
	protected static Element addCData(Document doc, Element parent, ConnectionInfo info) {
		Element cData = XmlUtils.createTag(doc, parent, "cData");
		addKeyValueTags(doc, cData, "hostname", info.host + ":" + info.port, "password", info.password, "username", info.username);
		return cData;
	}
	
	protected static Document doPostForXML(String requestBody, HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		PostMethod post = null;
		Document doc = null;
		try {
			post = doPost(requestBody, client, connectionInfo);
			doc = XmlUtils.parse(post.getResponseBodyAsStream());
		} finally {
			if (post != null) post.releaseConnection();
		}
		return doc;
	}
	
	protected static PostMethod doPost(String requestBody, HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		PostMethod post = new PostMethod(connectionInfo.endpoint);
		post.setRequestEntity(new StringRequestEntity(requestBody, "text/plain", "UTF-8"));
		client.executeMethod(post);
		return post;
	}
}
