package eu.openanalytics.phaedra.datacapture.columbus.montage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;
import eu.openanalytics.phaedra.datacapture.montage.layout.BaseFieldLayoutSource;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayout;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayoutCalculator;
import eu.openanalytics.phaedra.datacapture.util.VariableResolver;

/**
 * A layout source based on an Acapella MEAS file.
 * Only works if the reading-specific variable 'measFilePath' has been set.
 */
public class MeasFieldLayoutSource extends BaseFieldLayoutSource {

	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {
		String measFilePath = (String)VariableResolver.get("reading.measFilePath", context);
		FieldLayoutCalculator fieldCalc = new FieldLayoutCalculator(1);
		
		try (InputStream in = new FileInputStream(measFilePath)) {
			Document measDoc = XmlUtils.parse(in);
			NodeList areaTags = XmlUtils.findTags("/Measurement/Areas/Area", measDoc);
			Node areaTag = null;
			if (areaTags == null || areaTags.getLength() == 0) {
				throw new RuntimeException("Cannot determine field layout: no <Area> tag found in MEAS file");
			} else {
				// Use the first Area specified.
				areaTag = areaTags.item(0);
			}
			NodeList pointTags = XmlUtils.findTags("Sublayout/Point", areaTag);
			fieldCount = pointTags.getLength();
			for (int i=0; i<fieldCount; i++) {
				Element pointTag = (Element)pointTags.item(i);
				double x = Double.parseDouble(pointTag.getAttribute("x"));
				double y = Double.parseDouble(pointTag.getAttribute("y"));
				fieldCalc.addField(x, y, i+1);
			}
			return fieldCalc.calculate();
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse field layout from MEAS file", e);
		}
	}
}
