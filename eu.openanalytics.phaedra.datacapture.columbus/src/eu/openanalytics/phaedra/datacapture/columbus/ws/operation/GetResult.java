package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public class GetResult extends BaseOperation {

	private long resultId;
	private String resultValue;
	
	public GetResult(long resultId) {
		this.resultId = resultId;
	}
	
	@Override
	public void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		Document doc = XmlUtils.createEmptyDoc(true);
		Element functionTag = createFunctionTag(doc, "getResult");
		addCData(doc, functionTag, connectionInfo);
		addKeyValueTags(doc, functionTag, "resultId", String.valueOf(resultId), "formatType", "1");
		String requestBody = XmlUtils.writeToString(doc);
		
		doc = doPostForXML(requestBody, client, connectionInfo);
		resultValue = XmlUtils.findString("/Envelope/Body/getResultResponse/return", doc);
	}

	public String getResultValue() {
		return resultValue;
	}
}
