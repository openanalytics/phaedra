package eu.openanalytics.phaedra.base.ui.colormethod.lookup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;

public class LookupRuleParser {

	public List<LookupRule> parse(InputStream input) throws IOException {
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
	
	public List<LookupRule> parse(Document doc) {
		List<LookupRule> rules = new ArrayList<LookupRule>();
		
		NodeList ruleTags = XmlUtils.findTags("/rules/rule", doc);
		for (int i=0; i<ruleTags.getLength(); i++) {
			Node ruleTag = ruleTags.item(i);
			
			RGB color = new RGB(0,0,0);
			String colorString = XmlUtils.getNodeAttr(ruleTag, "color");
			if (colorString != null && !colorString.isEmpty()) {
				color = ColorUtils.parseRGBString(colorString);
			}
			
			String condition = XmlUtils.getNodeAttr(ruleTag, "condition");
			
			String valueString = XmlUtils.getNodeAttr(ruleTag, "value");
			double value = 0;
			if (NumberUtils.isDouble(valueString)) value = Double.parseDouble(valueString);
			
			LookupRule rule = new LookupRule(color, condition, value);
			rules.add(rule);
		}
		
		return rules;
	}
	
	public String write(List<LookupRule> rules) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<rules>");
		for (LookupRule rule: rules) {
			String colorString = ColorUtils.createRGBString(rule.getColor());
			sb.append("<rule color=\"" + colorString + "\"");
			sb.append(" condition=\"" + rule.getCondition() + "\"");
			sb.append(" value=\"" + rule.getValue() + "\" />");
		}
		sb.append("</rules>");
		
		return sb.toString();
	}
}
