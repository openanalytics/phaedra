package eu.openanalytics.phaedra.base.hdf5.parallel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.hdf5.Activator;
import eu.openanalytics.phaedra.base.util.process.comm.LocalProcessMaster;


public class HDF5MultiProcessReader extends LocalProcessMaster {

	private final static String[] PLUGINS = { Activator.PLUGIN_ID, "ch.systemsx.jhdf5", "org.apache.commons.io", "org.apache.commons.lang" };
	private final static String[] EXTRA_FILES = { "plugin://ch.systemsx.jhdf5/os/win32/x86_64/jhdf5.dll", "plugin://ch.systemsx.jhdf5/os/win32/x86_64/nativedata.dll" };
	
	public HDF5MultiProcessReader(int processes) {
		super(processes, PLUGINS, EXTRA_FILES, HDF5ReadProtocol.class.getName());
	}

	public Map<String, float[]> read(String filePath, List<String> features, int wellNr) {
		String req = HDF5ReadProtocol.CMD_READ_NUM + HDF5ReadProtocol.SEP + filePath + HDF5ReadProtocol.SEP + wellNr + HDF5ReadProtocol.SEP;
		if (features != null && !features.isEmpty()) {
			for (String f: features) req += f + HDF5ReadProtocol.SEP;
		}
		String res = sendRequest(req);
		if (res.startsWith("ERROR")) throw new RuntimeException(res);
		
		int wellNrResp = Integer.valueOf(res.substring(0, res.indexOf(HDF5ReadProtocol.SEP)));
		if (wellNr != wellNrResp) throw new RuntimeException("Well number mismatch: " + wellNr + " <> " + wellNrResp);
		res = res.substring(res.indexOf(HDF5ReadProtocol.SEP)+2);
		
		Map<String, float[]> retVal = new HashMap<>();
		String[] lines = res.split(HDF5ReadProtocol.SEP);
		for (String line: lines) {
			String[] parts = line.split("\t");
			String feature = parts[0];
			float[] data = new float[parts.length-1];
			for (int i = 0; i < data.length; i++) data[i] = Float.parseFloat(parts[i+1]);
			retVal.put(feature, data);
		}
		
		return retVal;
	}
}
