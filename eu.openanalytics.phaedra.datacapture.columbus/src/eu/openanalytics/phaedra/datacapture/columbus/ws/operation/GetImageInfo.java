package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public class GetImageInfo extends BaseOperation {

	private long imageId;
	private ImageInfo imageInfo;
	
	public GetImageInfo(long imageId) {
		this.imageId = imageId;
	}
	
	@Override
	public void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		Document doc = XmlUtils.createEmptyDoc(true);
		Element functionTag = createFunctionTag(doc, "getImageInfo");
		addCData(doc, functionTag, connectionInfo);
		addKeyValueTags(doc, functionTag, "imageId", String.valueOf(imageId));
		String requestBody = XmlUtils.writeToString(doc);
		
		doc = doPostForXML(requestBody, client, connectionInfo);
		String baseXPath = "/Envelope/Body/getImageInfoResponse/";
		imageInfo = new ImageInfo();
		imageInfo.width = XmlUtils.findInt(baseXPath + "width", doc);
		imageInfo.height = XmlUtils.findInt(baseXPath + "height", doc);
		imageInfo.channels = XmlUtils.findInt(baseXPath + "channels", doc);
		imageInfo.planes = XmlUtils.findInt(baseXPath + "planes", doc);
		imageInfo.timepoints = XmlUtils.findInt(baseXPath + "timepoints", doc);
		imageInfo.bpp = XmlUtils.findInt(baseXPath + "BitPerPixel", doc);
		imageInfo.resolutionX = XmlUtils.findDouble(baseXPath + "resolutionX", doc);
		imageInfo.resolutionY = XmlUtils.findDouble(baseXPath + "resolutionY", doc);
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}
	
	public static class ImageInfo {
		public int width;
		public int height;
		public int channels;
		public int planes;
		public int timepoints;
		public int bpp;
		public double resolutionX;
		public double resolutionY;
	}
}
