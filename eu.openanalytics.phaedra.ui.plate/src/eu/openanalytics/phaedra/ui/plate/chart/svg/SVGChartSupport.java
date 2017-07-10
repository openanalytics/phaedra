package eu.openanalytics.phaedra.ui.plate.chart.svg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class SVGChartSupport {

	private Plate plate;
	private String hdf5Path;
	private Map<String, byte[]> svgCache;
	
	private boolean chartsAvailable;
	private String[] availableChartNames;
	
	private Color bgColor;

	public SVGChartSupport(Plate plate) {
		this.plate = plate;
		this.chartsAvailable = false;
		this.hdf5Path = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
		this.svgCache = new HashMap<>();
		this.availableChartNames = new String[0];
		
		try {
			if (Screening.getEnvironment().getFileServer().exists(hdf5Path)) loadChartNames();
		} catch (IOException e) {
			EclipseLog.error("Failed to access HDF5 file", e, Activator.getDefault());
		}
	}

	public void setBgColor(Color bgColor) {
		if (this.bgColor != null && !this.bgColor.isDisposed()) this.bgColor.dispose();
		this.bgColor = bgColor;
	}

	public String[] getAvailableCharts() {
		return availableChartNames;
	}

	public Image getChart(String name, Well well, int w, int h) throws IOException {
		if (!chartsAvailable) return null;

		byte[] data = getChart(name, well);
		// Remove text from small charts
		if (w < 100 || h < 100) data = filterText(data);
		Image swtImage = ImageUtils.getSVGAsImage(data, w, h, bgColor);
		return swtImage;
	}

	public byte[] getChart(String name, Well well) throws IOException {
		if (!chartsAvailable) return null;
		
		int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
		String key = wellNr + "#" + name;

		byte[] data = svgCache.get(key);
		if (data == null) {
			synchronized(hdf5Path) {
				try (HDF5File dataFile = HDF5File.openForRead(hdf5Path)) {
					InputStream newData = dataFile.getExtraData(name + "/" + wellNr + ".svg");
					data = StreamUtils.readAll(newData);
				}
			}
			svgCache.put(key, data);
		}
		return data;
	}

	public void dispose() {
		svgCache.clear();
	}

	/*
	 * Non-public
	 */

	private void loadChartNames() {
		try (HDF5File dataFile = HDF5File.openForRead(hdf5Path)) {
			String[] extraData = dataFile.getChildren(HDF5File.getExtraDataPath());
			
			List<String> extraDataTemp = new ArrayList<>();
			for (String data : extraData) {
				if (!data.equals("Gates")) {
					extraDataTemp.add(data);
				}
			}
			
			availableChartNames = extraDataTemp.toArray(new String[extraDataTemp.size()]);
			chartsAvailable = true;
		} catch (Exception e) {
			availableChartNames = new String[0];
			chartsAvailable = false;
		}
	}
	
	private byte[] filterText(byte[] svg) {
		// Remove all text content from the SVG.
		try {
			InputStream input = new ByteArrayInputStream(svg);
	
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setNamespaceAware(false);
			factory.setValidating(false);
	
			Document doc = factory.newDocumentBuilder().parse(input);
			NodeList nodes = XmlUtils.findTags("//text", doc);
			for (int i=0; i<nodes.getLength(); i++) {
				nodes.item(i).getParentNode().removeChild(nodes.item(i));
			}
			String newDoc = XmlUtils.writeToString(doc);
			return newDoc.getBytes();
		} catch (Exception e) {
			return svg;
		}
	}

}
