package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public class GetAnalysis extends BaseOperation {

	private long analysisId;
	private String analysisValue;
	
	public GetAnalysis(long analysisId) {
		this.analysisId = analysisId;
	}
	
	@Override
	public void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		Document doc = XmlUtils.createEmptyDoc(true);
		Element functionTag = createFunctionTag(doc, "getAnalysis");
		addCData(doc, functionTag, connectionInfo);
		addKeyValueTags(doc, functionTag, "analysisId", String.valueOf(analysisId));
		String requestBody = XmlUtils.writeToString(doc);
		
		doc = doPostForXML(requestBody, client, connectionInfo);
		analysisValue = XmlUtils.findString("/Envelope/Body/getAnalysisResponse/return", doc);
	}

	public String getAnalysisValue() {
		return analysisValue;
	}
}
