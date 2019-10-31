package eu.openanalytics.phaedra.model.plate.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.model.plate.Activator;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlatePropertyDAO {

	public String getProperty(Plate plate, String name) {
		String xml = queryPropertyXML(plate.getId());
		if (xml != null) {
			try {
				Document doc = XmlUtils.parse(xml);
				return XmlUtils.findString("/data/properties/property[@key=\""
						+ name + "\"]/@value", doc);
			} catch (IOException e) {
				EclipseLog.warn("Failed to parse plate property XML: " + e.getMessage(), Activator.getDefault());
			}
		}
		return null;
	}

	public Map<String, String> getAllProperties(Plate plate) {
		String xml = queryPropertyXML(plate.getId());
		Map<String, String> properties = new HashMap<String, String>();
		if (xml != null) {
			try {
				Document doc = XmlUtils.parse(xml);
				NodeList tags = XmlUtils.findTags("/data/properties/property", doc);
				for (int i=0; i<tags.getLength(); i++) {
					Element tag = (Element)tags.item(i);
					String key = tag.getAttribute("key");
					String value = tag.getAttribute("value");
					properties.put(key, value);
				}
			} catch (IOException e) {
				EclipseLog.warn("Failed to parse plate property XML: " + e.getMessage(), Activator.getDefault());
			}
		}
		return properties;
	}

	public void setProperty(Plate plate, String name, String value) {
		String xml = queryPropertyXML(plate.getId());
		try {
			Document doc = null;
			if (xml == null) doc = XmlUtils.createEmptyDoc();
			else doc = XmlUtils.parse(xml);

			insertProperty(doc, name, value);

			xml = XmlUtils.writeToString(doc);
		} catch (IOException e) {
			throw new RuntimeException("Failed to update plate properties", e);
		}

		updatePropertyXML(plate.getId(), xml);
	}

	public void setProperties(Plate plate, Map<String,String> props) {
		String xml = queryPropertyXML(plate.getId());
		if (xml == null && props.isEmpty()) return;
		try {
			Document doc = null;
			if (xml == null) doc = XmlUtils.createEmptyDoc();
			else doc = XmlUtils.parse(xml);

			for (String name: props.keySet()) {
				insertProperty(doc, name, props.get(name));
			}

			xml = XmlUtils.writeToString(doc);
		} catch (IOException e) {
			throw new RuntimeException("Failed to update plate properties", e);
		}

		updatePropertyXML(plate.getId(), xml);
	}

	private String queryPropertyXML(long plateId) {
		try (Connection conn = getConnection()) {
			String sql = "select " + JDBCUtils.selectXMLColumn("p.data_xml") + " from phaedra.hca_plate p where p.plate_id = " + plateId;
			ResultSet rs = conn.createStatement().executeQuery(sql);
			if (rs.next()) return rs.getString(1);
			else return null;
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private void updatePropertyXML(long plateId, String xml) {
		try (Connection conn = getConnection()) {
			String sql = "update phaedra.hca_plate p set " + JDBCUtils.updateXMLColumn("data_xml") + " where p.plate_id = ?";
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setObject(1, JDBCUtils.getXMLObjectParameter(xml));
				ps.setLong(2, plateId);
				ps.addBatch();
				ps.execute();
			}
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private void insertProperty(Document doc, String key, String value) {

		// Make sure root "data" tag exists.
		NodeList dataTags = doc.getElementsByTagName("data");
		Element dataTag = null;
		if (dataTags.getLength() == 0) {
			dataTag = doc.createElement("data");
			doc.appendChild(dataTag);
		} else {
			dataTag = (Element)dataTags.item(0);
		}

		// Make sure "properties" tag exists.
		NodeList propertiesTags = dataTag.getElementsByTagName("properties");
		Element propertiesTag = null;
		if (propertiesTags.getLength() == 0) {
			propertiesTag = doc.createElement("properties");
			dataTag.appendChild(propertiesTag);
		} else {
			propertiesTag = (Element)propertiesTags.item(0);
		}

		// Look for "property" tag with this key. Update if exists, create if not.
		NodeList propertyTags = propertiesTag.getElementsByTagName("property");
		Element tagToUse = null;
		for (int i=0; i<propertyTags.getLength(); i++) {
			Element tag = (Element)propertyTags.item(i);
			if (tag.getAttribute("key").equals(key)) {
				tagToUse = tag;
				break;
			}
		}
		if (tagToUse == null) {
			tagToUse = doc.createElement("property");
			tagToUse.setAttribute("key", key);
			propertiesTag.appendChild(tagToUse);
		}
		tagToUse.setAttribute("value", value);
	}
	
	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
