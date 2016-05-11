package eu.openanalytics.phaedra.datacapture.config.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;

public class DataCaptureMappingParser {

	public Map<String,String> parse(InputStream input) throws IOException {
		Document doc = null;
		
		try {
			doc = XmlUtils.parse(input);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (input != null) try {input.close(); } catch (IOException e) {}
		}
		
		return parse(doc);
	}
	
	public Map<String,String> parse(Document doc) {
		Map<String,String> mappings = new HashMap<String, String>();
		
		NodeList mappingTags = XmlUtils.findTags("/data-capture-mappings/mapping", doc);
		for (int i=0; i<mappingTags.getLength(); i++) {
			Element e = (Element)mappingTags.item(i);
			String key = e.getAttribute("key");
			String value = e.getAttribute("capture-id");
			mappings.put(key, value);
		}
		
		return mappings;
	}
}
