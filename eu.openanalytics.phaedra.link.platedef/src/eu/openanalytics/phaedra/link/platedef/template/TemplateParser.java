package eu.openanalytics.phaedra.link.platedef.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;


public class TemplateParser {

	/*
	 * Reading
	 * *******
	 */
	
	public static PlateTemplate parse(InputStream input) throws IOException {
		PlateTemplate plate = new PlateTemplate();
		Document doc = readDoc(input);
		
		plate.setId(XmlUtils.findString("plate-template/@id", doc));
		plate.setCreator(XmlUtils.findString("plate-template/@creator", doc));
		String pClassId = XmlUtils.findString("plate-template/@protocolClassId", doc);
		if (pClassId != null && !pClassId.isEmpty()) {
			plate.setProtocolClassId(Long.parseLong(pClassId));
		}
		plate.setRows(XmlUtils.findInt("plate-template/dimensions/@rows", doc));
		plate.setColumns(XmlUtils.findInt("plate-template/dimensions/@columns", doc));
		
		Map<Integer, WellTemplate> wells = new HashMap<Integer, WellTemplate>();
		plate.setWells(wells);
		
		NodeList wellTags = XmlUtils.findTags("plate-template/wells/well", doc);
		for (int i=0; i<wellTags.getLength(); i++) {
			Element wellTag = (Element)wellTags.item(i);
			WellTemplate well = new WellTemplate();
			String wellNr = XmlUtils.getNodeAttr(wellTag, "nr");
			if (wellNr != null) {
				well.setNr(Integer.parseInt(wellNr));
			}
			Node controlTag = XmlUtils.getNodeByName(wellTag, "control");
			if (controlTag != null) {
				well.setWellType(XmlUtils.getNodeValue(controlTag));
			}
			Node compoundTypeTag = XmlUtils.getNodeByName(wellTag, "compound-type");
			if (compoundTypeTag != null) {
				well.setCompoundType(XmlUtils.getNodeValue(compoundTypeTag));
			}
			Node compoundNrTag = XmlUtils.getNodeByName(wellTag, "compound-number");
			if (compoundNrTag != null) {
				well.setCompoundNumber(XmlUtils.getNodeValue(compoundNrTag));
			}
			Node concTag = XmlUtils.getNodeByName(wellTag, "compound-concentration");
			if (concTag != null) {
				well.setConcentration(XmlUtils.getNodeValue(concTag));
			}
			Node remarkTag = XmlUtils.getNodeByName(wellTag, "remark");
			if (remarkTag != null) {
				well.setRemark(XmlUtils.getNodeValue(remarkTag));
			}
			Node skipTag = XmlUtils.getNodeByName(wellTag, "skip");
			if (skipTag != null) {
				String strSkip = XmlUtils.getNodeValue(skipTag);
				well.setSkip(Boolean.valueOf(strSkip));
			}
			
			NodeList annotationTags = XmlUtils.findTags("annotations/annotation", wellTag);
			for (int j=0; j<annotationTags.getLength(); j++) {
				Element annotationTag = (Element)annotationTags.item(j);
				String ann = annotationTag.getAttribute("name");
				String value = XmlUtils.getNodeValue(annotationTag);
				well.getAnnotations().put(ann, value);
			}
			
			wells.put(well.getNr(), well);
		}
		
		return plate;
	}
	
	private static Document readDoc(InputStream input) throws IOException {
		try {
			// First to string to avoid encoding issues (micro sign etc).
			String xmlString = new String(StreamUtils.readAll(input));
			Document doc = XmlUtils.parse(xmlString);
			return doc;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (input != null) input.close();
		}
	}
	
	/*
	 * Writing
	 * *******
	 */

	public static void write(PlateTemplate plate, OutputStream out) throws IOException {
		try {
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
			writer.writeStartDocument();
			writer.writeCharacters("\r\n");
			writer.writeStartElement("plate-template");
			writer.writeAttribute("id", plate.getId());
			if (plate.getCreator() != null) writer.writeAttribute("creator", plate.getCreator());
			if (plate.getProtocolClassId() > 0) writer.writeAttribute("protocolClassId", ""+plate.getProtocolClassId()); 
			writer.writeCharacters("\r\n");
			
			// Dimensions tag
			writer.writeStartElement("dimensions");
			writer.writeAttribute("rows", ""+plate.getRows());
			writer.writeAttribute("columns", ""+plate.getColumns());
			writer.writeEndElement();
			writer.writeCharacters("\r\n");
			
			// Wells tag
			writer.writeStartElement("wells");
			writer.writeCharacters("\r\n");
			for (WellTemplate well: plate.getWells().values()) {
				writer.writeStartElement("well");
				writer.writeAttribute("nr", ""+well.getNr());
				writer.writeCharacters("\r\n");
				
				if (well.getWellType() != null) {
					writer.writeStartElement("control");
					writer.writeCharacters(well.getWellType());
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				if (well.getCompoundType() != null) {
					writer.writeStartElement("compound-type");
					writer.writeCharacters(well.getCompoundType());
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				if (well.getCompoundNumber() != null) {
					writer.writeStartElement("compound-number");
					writer.writeCharacters(well.getCompoundNumber());
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				if (well.getConcentration() != null) {
					writer.writeStartElement("compound-concentration");
					writer.writeCharacters(well.getConcentration());
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				if (well.getRemark() != null) {
					writer.writeStartElement("remark");
					writer.writeCharacters(well.getRemark());
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				boolean skip = well.isSkip();
				writer.writeStartElement("skip");
				writer.writeCharacters(skip?"true":"false");
				writer.writeEndElement();
				writer.writeCharacters("\r\n");

				if (well.getAnnotations() != null && !well.getAnnotations().isEmpty()) {
					writer.writeStartElement("annotations");
					for(String ann: well.getAnnotations().keySet()) {
						String value = well.getAnnotations().get(ann);
						if (value == null || value.isEmpty()) continue;
						writer.writeStartElement("annotation");
						writer.writeAttribute("name", ann);
						writer.writeCharacters(value);
						writer.writeEndElement();
					}
					writer.writeEndElement();
					writer.writeCharacters("\r\n");
				}
				
				writer.writeEndElement();
				writer.writeCharacters("\r\n");
			}
			writer.writeEndElement();
			writer.writeCharacters("\r\n");
			
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();
		} catch (XMLStreamException e) {
			throw new IOException("Error while writing XML template", e);
		}		
	}
	

}
