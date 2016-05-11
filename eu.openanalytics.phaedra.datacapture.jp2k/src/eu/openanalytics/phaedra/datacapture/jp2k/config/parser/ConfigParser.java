package eu.openanalytics.phaedra.datacapture.jp2k.config.parser;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.jp2k.config.ComponentConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.ComponentFileConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.CompressionConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.Config;

public class ConfigParser {
	
	public Config parse(Node configNode) {

		Config config = new Config();
		
		NodeList compressionTags = XmlUtils.findTags("compression", configNode);
		if (compressionTags.getLength() > 0) {
			Element compressionTag = (Element)compressionTags.item(0);
			config.defaultCompression = parseCompression(compressionTag);
		}
		
		NodeList componentTags = XmlUtils.findTags("components/component", configNode);
		ComponentConfig[] comps = new ComponentConfig[componentTags.getLength()];
		for (int i=0; i<componentTags.getLength(); i++) {
			Element componentTag = (Element)componentTags.item(i);
			comps[i] = parseComponent(componentTag);
			comps[i].id = i;
		}
		config.components = comps;
		
		return config;
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private CompressionConfig parseCompression(Element compressionTag) {
		CompressionConfig config = new CompressionConfig();
		
		config.levels = getIntAttribute("levels", compressionTag);
		config.slope = getIntAttribute("slope", compressionTag);
		config.psnr = getIntAttribute("psnr", compressionTag);
		
		String stringVal = compressionTag.getAttribute("type");
		config.type = stringVal.isEmpty() ? null : stringVal;
		stringVal = compressionTag.getAttribute("order");
		config.order = stringVal.isEmpty() ? null : stringVal;
		stringVal = compressionTag.getAttribute("precincts");
		config.precincts = stringVal.isEmpty() ? null : stringVal;
		
		return config;
	}
	
	private ComponentConfig parseComponent(Element componentTag) {
		ComponentConfig config = new ComponentConfig();
		
		Element compressionTag = (Element)XmlUtils.getNodeByName(componentTag, "compression");
		if (compressionTag != null) {
			config.compression = parseCompression(compressionTag);
		}

		Element convertTag = (Element)XmlUtils.getNodeByName(componentTag, "convert");
		if (convertTag != null) {
			config.convert = true;
			config.convertArgs = XmlUtils.getNodeValue(convertTag);
			if (config.convertArgs == null) config.convertArgs = "";
			
			String frame = convertTag.getAttribute("frame");
			if (frame != null && !frame.isEmpty()) config.convertFrame = frame;
			
			Element convertArgsOnFail = (Element)XmlUtils.getNodeByName(componentTag, "convert-onfail");
			if (convertArgsOnFail != null) config.convertArgsOnFail = XmlUtils.getNodeValue(convertArgsOnFail);
		}
		
		NodeList componentFileTags = XmlUtils.findTags("files", componentTag);
		ComponentFileConfig[] files = new ComponentFileConfig[componentFileTags.getLength()];
		for (int i=0; i<componentFileTags.getLength(); i++) {
			Element componentFileTag = (Element)componentFileTags.item(i);
			files[i] = parseComponentFile(componentFileTag);
		}
		config.files = files;
		
		return config;
	}
	
	private ComponentFileConfig parseComponentFile(Element componentFileTag) {
		ComponentFileConfig fileConfig = new ComponentFileConfig();
		
		fileConfig.path = componentFileTag.getAttribute("path");
		fileConfig.pattern = componentFileTag.getAttribute("pattern");
		fileConfig.patternIdGroups = componentFileTag.getAttribute("pattern-id-groups");
		fileConfig.idGroup = getIntAttribute("id-group", componentFileTag);
		
		return fileConfig;
	}
	
	private int getIntAttribute(String name, Element tag) {
		String value = tag.getAttribute(name);
		if (value != null && NumberUtils.isDouble(value)) return Integer.parseInt(value);
		return 0;
	}
}
