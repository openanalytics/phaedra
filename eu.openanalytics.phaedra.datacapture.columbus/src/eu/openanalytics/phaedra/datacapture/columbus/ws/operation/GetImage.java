package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public class GetImage extends BaseOperation {

	private long imageId;
	private OutputStream out;
	
	public GetImage(long imageId, OutputStream out) {
		this.imageId = imageId;
		this.out = out;
	}
	
	@Override
	public void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		
		// Step 1: request an image transfer.
		Document doc = XmlUtils.createEmptyDoc(true);
		Element functionTag = createFunctionTag(doc, "readImage");
		addCData(doc, functionTag, connectionInfo);
		addKeyValueTags(doc, functionTag, "imageId", String.valueOf(imageId), "formatType", "1", "compress", "false");
		String requestBody = XmlUtils.writeToString(doc);
		
		doc = doPostForXML(requestBody, client, connectionInfo);
		String transferId = XmlUtils.findString("/Envelope/Body/readImageResponse/transferId", doc);
		String size = XmlUtils.findString("/Envelope/Body/readImageResponse/size", doc);
		
		// Step 2: perform the image transfer
		doc = XmlUtils.createEmptyDoc(true);
		functionTag = createFunctionTag(doc, "transfer");
		addKeyValueTags(doc, functionTag, "transferId", transferId, "size", size);
		requestBody = XmlUtils.writeToString(doc);
		
		PostMethod post = null;
		try {
			post = doPost(requestBody, client, connectionInfo);
			String contentType = post.getResponseHeader("Content-Type").getValue();
			String boundary = contentType.substring(contentType.indexOf("boundary=") + 9, contentType.indexOf("; type="));
			try (OutputStream outWrapper = new SkippingOutputStream(new BufferedOutputStream(out), ">\r\n\r\n")) {
				MultipartStream multipartStream = new MultipartStream(post.getResponseBodyAsStream(), boundary.getBytes(), 4096, null);
				multipartStream.skipPreamble();
				multipartStream.discardBodyData();
				multipartStream.readBoundary();
				multipartStream.readBodyData(outWrapper);
			}
		} finally {
			if (post != null) post.releaseConnection();
		}
		
		// Step 3: close the image transfer
		doc = XmlUtils.createEmptyDoc(true);
		functionTag = createFunctionTag(doc, "closeTransfer");
		addKeyValueTags(doc, functionTag, "transferId", transferId);
		requestBody = XmlUtils.writeToString(doc);
		
		doPost(requestBody, client, connectionInfo).releaseConnection();
	}

	private class SkippingOutputStream extends FilterOutputStream {

		private boolean skipping;
		private String token;
		private char[] current;
		
		public SkippingOutputStream(OutputStream out, String token) {
			super(out);
			this.skipping = true;
			this.token = token;
			this.current = new char[token.length()];
		}
		
		@Override
		public void write(int b) throws IOException {
			if (skipping) {
				// Shift current
				for (int i=0; i<(current.length-1); i++) current[i] = current[i+1];
				current[current.length-1] = (char)b;
				// Compare current to token
				String currentString = new String(current);
				skipping = !currentString.equals(token);
			} else {
				super.write(b);
			}
		}
	}
}
