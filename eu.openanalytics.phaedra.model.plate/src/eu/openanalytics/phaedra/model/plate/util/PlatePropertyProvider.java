package eu.openanalytics.phaedra.model.plate.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlatePropertyProvider {

	private static final String SEQUENCE = "Sequence";
	private static final String BARCODE = "Barcode";
	private static final String DESCRIPTION = "Description";
	private static final String LINK_INFO = "Link Info";
	private static final String CALCULATION = "Calculation";
	private static final String EXPERIMENT = "Experiment";
	private static final String PROTOCOL = "Protocol";
	private static final String PROTOCOL_CLASS = "Protocol Class";
	private static final String SIZE_SUBWELL_DATA = "Size Subwell Data";
	private static final String SIZE_IMAGE_DATA = "Size Image Data";
	private static final String SUBWELL_DATA_FILE = "Subwell Data File";

	public static String[] getKeys(Plate p) {
		List<String> keys = new ArrayList<>();
		if (p != null) {
			keys.add(SEQUENCE);
			keys.add(BARCODE);
			keys.add(DESCRIPTION);
			keys.add(LINK_INFO);
			keys.add(CALCULATION);
			keys.add(EXPERIMENT);
			keys.add(PROTOCOL);
			keys.add(PROTOCOL_CLASS);
			keys.add(SIZE_SUBWELL_DATA);
			keys.add(SIZE_IMAGE_DATA);
			keys.add(SUBWELL_DATA_FILE);

			Map<String,String> props = PlateService.getInstance().getPlateProperties(p);
			for (String prop : props.keySet()) {
				if (!prop.isEmpty()) keys.add(prop);
			}
		}

		String[] keyArray = keys.toArray(new String[keys.size()]);
		Arrays.sort(keyArray);
		return keyArray;

	}

	public static String getValue(String key, Plate p) {
		switch(key) {
		case SEQUENCE: return "" + p.getSequence();
		case BARCODE: return p.getBarcode();
		case DESCRIPTION: return p.getDescription() == null ? "" : p.getDescription();
		case LINK_INFO: return p.getInfo() == null ? "" : p.getInfo();
		case CALCULATION: return p.getCalculationDate() == null ? "" : p.getCalculationDate().toString();
		case EXPERIMENT: return p.getExperiment().toString();
		case PROTOCOL: return p.getExperiment().getProtocol().toString();
		case PROTOCOL_CLASS: return PlateUtils.getProtocolClass(p).toString();
		case SIZE_SUBWELL_DATA:
			String hdf5Path = PlateService.getInstance().getPlateFSPath(p, true);
			return getFileSize(hdf5Path + "/" + p.getId() + ".h5");
		case SIZE_IMAGE_DATA:
			String imgPath = PlateService.getInstance().getImagePath(p);
			return getFileSize(imgPath);
		case SUBWELL_DATA_FILE:
			String path = PlateService.getInstance().getPlateFSPath(p, true) + "/" + p.getId() + ".h5";
			return path.replace('/', '\\');
		default:
			Map<String,String> props = PlateService.getInstance().getPlateProperties(p);
			return props.get(key);
		}
	}

	private static String getFileSize(String fileName) {
		if (fileName != null) {
			File f = new File(fileName);
			return FileUtils.getHumanReadableByteCount(f.length(), false);
		}
		return "";
	}

}
