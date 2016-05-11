package eu.openanalytics.phaedra.ui.plate.metadata;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellMetaDataCalculator {

	private List<Well> currentWells = null;

	private Set<String> keywords = new HashSet<>();
	private Map<String, Map<String, String>> mappedKeywords = new HashMap<>();

	public void calculate() {
		if (currentWells != null && !currentWells.isEmpty()) {
			keywords.clear();
			mappedKeywords.clear();
			Plate plate = currentWells.get(0).getPlate();
			String hdf5Path = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
			if (new File(hdf5Path).isFile()) {
				HDF5File file = new HDF5File(hdf5Path, true);
				if (!file.hasChildren(HDF5File.getWellMetaDataPath())) {
					mappedKeywords = file.getMetaData();
					for (String wellNr : mappedKeywords.keySet())
						for (String key : mappedKeywords.get(wellNr).keySet())
							if (!key.equals("Wells")) keywords.add(key);
				} else {
					// Legacy support
					for (Well well : currentWells) {
						int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
						Map<String, String> mappedValues = new HashMap<String, String>();
						mappedKeywords.put(wellNr + "", mappedValues);
	
						if (!file.exists(HDF5File.getWellMetaDataPath() + "/" + wellNr))
							continue;
	
						String[] metanames = file.getAttributes(HDF5File.getWellMetaDataPath() + "/" + wellNr);
						for (String metaname : metanames) {
							keywords.add(metaname);
							Object value = file.getAttribute(HDF5File.getWellMetaDataPath() + "/" + wellNr, metaname);
							if (value == null) value = "";
							mappedValues.put(metaname, value.toString());
						}
					}
				}
				file.close();
			}
		}
	}

	public List<Well> getCurrentWells() {
		return currentWells;
	}

	public void setCurrentWells(List<Well> currentWells) {
		this.currentWells = currentWells;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public Map<String, Map<String, String>> getMappedKeywords() {
		return mappedKeywords;
	}

}