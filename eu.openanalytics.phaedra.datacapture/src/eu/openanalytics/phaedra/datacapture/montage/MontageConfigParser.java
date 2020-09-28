package eu.openanalytics.phaedra.datacapture.montage;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig.ImageComponent;

public class MontageConfigParser {
	
	public MontageConfig parse(Node configNode) {

		MontageConfig config = new MontageConfig();

		Element montageTag = XmlUtils.getFirstElement("montage", configNode);
		config.layoutSource = montageTag.getAttribute("layoutSource");
		config.layout = montageTag.getAttribute("layout");
		config.padding = XmlUtils.getIntegerAttribute(montageTag, "padding");
		config.startingFieldNr = XmlUtils.getIntegerAttribute(montageTag, "starting-field-nr");

		Element swTag = XmlUtils.getFirstElement("subwelldata", configNode);
		if (swTag != null) {
			config.subwellDataPath = swTag.getAttribute("path");
			config.subwellDataPattern = swTag.getAttribute("pattern");
			config.subwellDataPatternIdGroups = swTag.getAttribute("pattern-id-groups");
			config.subwellDataPatternFieldGroup = swTag.getAttribute("pattern-field-group");
			config.subwellDataOutput = swTag.getAttribute("output");
		
			Element parserTag = XmlUtils.getFirstElement("parser", swTag);
			if (parserTag != null) {
				config.subwellDataParserId = parserTag.getAttribute("id");
			}
		
			Element featuresTag = XmlUtils.getFirstElement("x-features", swTag);
			List<String> features = new ArrayList<>();
			NodeList featureTags = XmlUtils.findTags("feature", featuresTag);
			for (int i=0; i<featureTags.getLength(); i++) {
				features.add(featureTags.item(i).getTextContent());
			}
			config.subwellDataXFeatures = features.toArray(new String[features.size()]);
			
			features.clear();
			featureTags = XmlUtils.findTags("featurePattern", featuresTag);
			for (int i=0; i<featureTags.getLength(); i++) {
				features.add(featureTags.item(i).getTextContent());
			}
			config.subwellDataXFeaturePatterns = features.toArray(new String[features.size()]);
			
			features.clear();
			featuresTag = XmlUtils.getFirstElement("y-features", swTag);
			featureTags = XmlUtils.findTags("feature", featuresTag);
			for (int i=0; i<featureTags.getLength(); i++) {
				features.add(featureTags.item(i).getTextContent());
			}
			config.subwellDataYFeatures = features.toArray(new String[features.size()]);
			
			features.clear();
			featureTags = XmlUtils.findTags("featurePattern", featuresTag);
			for (int i=0; i<featureTags.getLength(); i++) {
				features.add(featureTags.item(i).getTextContent());
			}
			config.subwellDataYFeaturePatterns = features.toArray(new String[features.size()]);
		}
		
		List<ImageComponent> components = new ArrayList<>();
		Element imageTag = XmlUtils.getFirstElement("imagedata", configNode);
		if (imageTag != null) {
			NodeList componentTags = XmlUtils.findTags("components/component", imageTag);
			for (int i=0; i<componentTags.getLength(); i++) {
				Element componentTag = (Element)componentTags.item(i);
				ImageComponent component = new ImageComponent();
				component.path = componentTag.getAttribute("path");
				component.pattern = componentTag.getAttribute("pattern");
				component.patternIdGroups = componentTag.getAttribute("pattern-id-groups");
				component.patternFieldGroup = componentTag.getAttribute("pattern-field-group");
				component.frame = componentTag.getAttribute("frame");
				component.output = componentTag.getAttribute("output");
				component.overlay = Boolean.valueOf(componentTag.getAttribute("overlay"));
				components.add(component);
			}
			config.imageComponents = components.toArray(new ImageComponent[components.size()]);
		}
		
		return config;
	}
}
