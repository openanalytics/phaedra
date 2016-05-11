package eu.openanalytics.phaedra.calculation.stat.persist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.cache.StatContainer;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PersistentPlateStats {

	private Map<Long, StatContainer> featureStats;
	private Map<String, StatContainer> controlStats;

	public final static PersistentPlateStats EMPTY_STATS = new PersistentPlateStats();

	public final static String[] FEATURE_STATS = {"zprime","sb","sn"};
	public final static String[] CONTROL_STATS = {
		"mean","stdev","median","min","max",
		"skewness","kurtosis","cv","count"};

	public PersistentPlateStats() {
		featureStats = new ConcurrentHashMap<Long, StatContainer>();
		controlStats = new ConcurrentHashMap<String, StatContainer>();
	}

	public String[] getWellTypes() {
		Set<String> controlKeys = controlStats.keySet();
		List<String> wellTypes = new ArrayList<String>();
		for (String key: controlKeys) {
			String[] parts = key.split("#");
			if (parts.length > 1 && parts[1] != null && !parts[1].isEmpty())
				CollectionUtils.addUnique(wellTypes, parts[1]);
		}
		return wellTypes.toArray(new String[wellTypes.size()]);
	}

	public StatContainer getStats(Feature f) {
		return featureStats.get(f.getId());
	}

	public StatContainer getStats(Feature f, String wellType) {
		String key = f.getId() + "#" + wellType;
		return controlStats.get(key);
	}

	public void setStats(Feature f, StatContainer container) {
		featureStats.put(f.getId(), container);
	}

	public void setStats(Feature f, String wellType, StatContainer container) {
		String key = f.getId() + "#" + wellType;
		controlStats.put(key, container);
	}

	public void loadFromXml(String xml) throws IOException {
		if (xml == null || xml.isEmpty()) return;
		Document doc = XmlUtils.parse(xml);

		NodeList statTags = XmlUtils.findTags("/data/statistics/statistic", doc);
		for (int i=0; i<statTags.getLength(); i++) {
			Element e = (Element)statTags.item(i);
			long fId = Long.parseLong(e.getAttribute("feature-id"));
			StatContainer container = new StatContainer();
			featureStats.put(fId, container);

			for (String stat: FEATURE_STATS) {
				String value = e.getAttribute(translateStatName(stat));
				if (value != null && !value.isEmpty()) container.add(stat, Double.parseDouble(value));
			}
		}

		NodeList controlTags = XmlUtils.findTags("/data/controls/control", doc);
		for (int i=0; i<controlTags.getLength(); i++) {
			Element e = (Element)controlTags.item(i);
			long fId = Long.parseLong(e.getAttribute("feature-id"));
			String wellType = e.getAttribute("welltype");
			StatContainer container = new StatContainer();
			controlStats.put(fId+"#"+wellType, container);

			for (String stat: CONTROL_STATS) {
				String value = e.getAttribute(translateStatName(stat));
				if (value != null && !value.isEmpty()) container.add(stat, Double.parseDouble(value));
			}
		}
	}

	public void writeToXml(Document doc) {
		NodeList dataTags = doc.getElementsByTagName("data");
		Element dataTag = null;
		if (dataTags.getLength() == 0) {
			dataTag = doc.createElement("data");
			doc.appendChild(dataTag);
		} else {
			dataTag = (Element)dataTags.item(0);
		}

		NodeList statsTags = dataTag.getElementsByTagName("statistics");
		Element statsTag = null;
		if (statsTags.getLength() == 0) {
			statsTag = doc.createElement("statistics");
			dataTag.appendChild(statsTag);
		} else {
			statsTag = (Element)statsTags.item(0);
		}

		NodeList statTags = statsTag.getElementsByTagName("statistic");
		for (long featureId: featureStats.keySet()) {
			Element tagToUse = null;
			for (int i=0; i<statTags.getLength(); i++) {
				Element tag = (Element)statTags.item(i);
				if (tag.getAttribute("feature-id").equals(""+featureId)) {
					tagToUse = tag;
					break;
				}
			}
			if (tagToUse == null) {
				tagToUse = doc.createElement("statistic");
				tagToUse.setAttribute("feature-id", ""+featureId);
				statsTag.appendChild(tagToUse);
			}
			StatContainer container = featureStats.get(featureId);
			for (String stat: FEATURE_STATS) {
				tagToUse.setAttribute(translateStatName(stat), StatUtils.format(container.get(stat)));
			}
		}

		NodeList controlsTags = dataTag.getElementsByTagName("controls");
		Element controlsTag = null;
		if (controlsTags.getLength() == 0) {
			controlsTag = doc.createElement("controls");
			dataTag.appendChild(controlsTag);
		} else {
			controlsTag = (Element)controlsTags.item(0);
		}

		NodeList controlTags = controlsTag.getElementsByTagName("control");
		for (String key: controlStats.keySet()) {
			String[] keyParts = key.split("#");
			Element tagToUse = null;
			for (int i=0; i<controlTags.getLength(); i++) {
				Element tag = (Element)controlTags.item(i);
				if (tag.getAttribute("feature-id").equals(keyParts[0])
						&& tag.getAttribute("welltype").equals(keyParts[1])) {
					tagToUse = tag;
					break;
				}
			}
			if (tagToUse == null) {
				tagToUse = doc.createElement("control");
				tagToUse.setAttribute("feature-id", keyParts[0]);
				tagToUse.setAttribute("welltype", keyParts[1]);
				controlsTag.appendChild(tagToUse);
			}
			StatContainer container = controlStats.get(key);
			for (String stat: CONTROL_STATS) {
				tagToUse.setAttribute(translateStatName(stat), StatUtils.format(container.get(stat)));
			}
		}
	}

	private String translateStatName(String name) {
		if (name.equals("max")) return "maximum";
		if (name.equals("min")) return "minimum";
		return name;
	}
}
