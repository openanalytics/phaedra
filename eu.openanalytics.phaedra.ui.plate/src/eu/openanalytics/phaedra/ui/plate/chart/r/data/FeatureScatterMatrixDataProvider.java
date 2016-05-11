package eu.openanalytics.phaedra.ui.plate.chart.r.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProviderR;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeatureScatterMatrixDataProvider implements IDataProviderR {

	private static final String WELLTYPE = "Welltype";
	private static final String FEATURES = "Features";

	private List<Plate> plates;
	private List<Feature> features;

	public FeatureScatterMatrixDataProvider(List<Plate> plates, List<Feature> features) {
		this.plates = plates;
		this.features = features;
	}

	@Override
	public Map<String, List<Object>> getDataFrame() {
		Map<String, List<Object>> dataFrame = new HashMap<>();
		dataFrame.put(WELLTYPE, new ArrayList<>());
		dataFrame.put(FEATURES, new ArrayList<>());

		int i = 1;
		for (Feature feature : features) {
			dataFrame.get(FEATURES).add(feature.getDisplayName());
			dataFrame.put("F" + i++, new ArrayList<>());
		}

		for (Plate plate : plates) {
			i = 1;
			for (Feature feature : features) {
				for (String type : PlateUtils.getWellTypes(plate)) {
					double sol = StatService.getInstance().calculate("mean", plate, feature, type, feature.getNormalization());
					if (Double.isNaN(sol)) sol = 0;
					dataFrame.get("F" + i).add(sol);
					dataFrame.get(WELLTYPE).add(type);
				}
				i++;
			}
		}

		List<Integer> featuresToRemove = new ArrayList<>();
		for (i = 0; i < features.size(); i++) {
			List<Object> value = dataFrame.get("F" + (i-1));
			if (value != null && value.isEmpty()) {
				featuresToRemove.add(i);
			}
		}
		for (int featureToRemove: featuresToRemove) {
			dataFrame.remove("F" + (featureToRemove+1));
			dataFrame.get(FEATURES).remove(features.get(featureToRemove).getDisplayName());
			features.remove(featureToRemove);
		}
		return dataFrame;
	}

}