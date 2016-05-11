package eu.openanalytics.phaedra.base.hdf5.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.process.comm.BaseLocalProcessProtocol;

public class HDF5ReadProtocol extends BaseLocalProcessProtocol {

	public final static String CMD_READ_NUM = "read_num";
	public final static String SEP = "##";

	@Override
	public String process(String input) {
		if (input.startsWith(CMD_READ_NUM)) {
			String[] parts = input.split(SEP);
			String filePath = parts[1];
			int wellNr = Integer.parseInt(parts[2]);

			List<String> features = new ArrayList<>();
			int featureCount = parts.length - 3;
			for (int i=0; i<featureCount; i++) {
				String fName = parts[i+3];
				if (fName != null && !fName.isEmpty()) features.add(fName);
			}

			StringBuilder res = new StringBuilder();
			res.append(wellNr + SEP);
			try (HDF5File file = new HDF5File(filePath, true)) {
				if (features.isEmpty()) features = file.getSubWellFeatures();
				for (String f: features) {
					res.append(f + "\t");
					// Check if the data exists, if not add no data.
					if (file.existsSubWellData(f, wellNr)) {
						if (file.isSubWellDataNumeric(f, wellNr)) {
							float[] data = file.getNumericSubWellData(f, wellNr);
							for (float d: data) res.append(d + "\t");
						} else {
							String[] data = file.getStringSubWellData(f, wellNr);
							for (String d: data) res.append(d + "\t");
						}
					}
					res.append(SEP);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return res.toString();
		}
		else return super.process(input);
	}
}