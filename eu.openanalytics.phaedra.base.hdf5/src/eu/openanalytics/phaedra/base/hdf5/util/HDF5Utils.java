package eu.openanalytics.phaedra.base.hdf5.util;

import java.io.IOException;
import java.util.List;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

public class HDF5Utils {

	public HDF5File open(String path) {
		return new HDF5File(path, true);
	}
	
	public int[] guessLayout(HDF5File file) throws IOException {
		int wellCount = 0;
		
		// First look at the available well features.
		List<String> wellFeatures = file.getWellFeatures();
		for (String feature: wellFeatures) {
			boolean numeric = file.isWellDataNumeric(feature);
			int count = 0;
			if (numeric) count = file.getNumericWellData(feature).length;
			else count = file.getStringWellData(feature).length;
			if (count > wellCount) wellCount = count;
		}
		
		// Then look at the available subwell features.
		List<String> subwellFeatures = file.getSubWellFeatures();
		for (String feature: subwellFeatures) {
			String path = HDF5File.getSubWellDataPath(1, feature);
			path = FileUtils.getPath(path);
			int count = file.getChildren(path).length;
			if (count > wellCount) wellCount = count;
		}
		
		int[] layout = new int[2];
		if (wellCount <= 96) {
			layout[0] = 8;
			layout[1] = 12;
		} else if (wellCount <= 384) {
			layout[0] = 16;
			layout[1] = 24;
		} else {
			layout[0] = 32;
			layout[1] = 48;
		}
		
		return layout;
	}
}
