package eu.openanalytics.phaedra.datacapture.config.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.config.ParameterGroup;

public class ModuleConfigParser {

	public CaptureConfig parse(InputStream input) throws IOException {
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
	
	public CaptureConfig parse(Document doc) {

		List<ModuleConfig> configs = new ArrayList<ModuleConfig>();
		
		CaptureConfig config = new CaptureConfig();
		
		NodeList parametersTags = XmlUtils.findTags("/data-capture-config/parameters", doc);
		if (parametersTags.getLength() > 0) {
			Element parametersTag = (Element)parametersTags.item(0);
			config.setParameters(createParameters(parametersTag));
		} else {
			config.setParameters(new ParameterGroup());
		}
		
		NodeList moduleTags = XmlUtils.findTags("/data-capture-config/module", doc);
		for (int i=0; i<moduleTags.getLength(); i++) {
			Node moduleTag = moduleTags.item(i);
			ModuleConfig cfg = createModuleConfig((Element)moduleTag);
			cfg.setParentConfig(config);
			configs.add(cfg);
		}
		
		config.setModuleConfigs(configs.toArray(new ModuleConfig[configs.size()]));
		return config;
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private ModuleConfig createModuleConfig(Element moduleTag) {
		ModuleConfig config = new ModuleConfig();
		
		config.setName(moduleTag.getAttribute("name"));
		config.setType(moduleTag.getAttribute("type"));
		config.setId(moduleTag.getAttribute("id"));
		
		Node parametersTag = XmlUtils.getNodeByName(moduleTag, "parameters");
		if (parametersTag != null) {
			ParameterGroup params = createParameters((Element)parametersTag);
			config.setParameters(params);
		} else {
			config.setParameters(new ParameterGroup());
		}
		
		return config;
	}
	
	private ParameterGroup createParameters(Element parametersTag) {
		ParameterGroup group = new ParameterGroup();
		
		NodeList parameterTags = parametersTag.getElementsByTagName("parameter");
		for (int i=0; i<parameterTags.getLength(); i++) {
			Element parameterTag = (Element)parameterTags.item(i);
			String key = parameterTag.getAttribute("key");
			NodeList contents = parameterTag.getChildNodes();
			int len = contents.getLength();
			Object value = null;
			if (len == 1) {
				// Assume text contents
				value = XmlUtils.getNodeValue(parameterTag);
			} else {
				// Sub-structure, pass entire XML tag
				value = parameterTag;
			}
			if (key != null && value != null) {
				group.setParameter(key, value);
			}
		}
		
		return group;
	}
}
