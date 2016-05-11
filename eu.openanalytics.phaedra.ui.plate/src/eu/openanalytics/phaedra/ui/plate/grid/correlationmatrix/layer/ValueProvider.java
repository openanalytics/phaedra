package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer;

import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS_STANDARD_ERRORS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.SPEARMANS;

import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class ValueProvider {

	public static String getValue(int type, RealMatrix matrix, int col, int row, List<? extends IFeature> features) {
		if (matrix == null) return "";

		switch (type) {
		case 0: return "";
		case 1: return getFeatureLabels(features);
		case 2: return NumberUtils.round(CorrelationProvider.getValue(PEARSONS, matrix, col, row), 2);
		case 3: return NumberUtils.round(CorrelationProvider.getValue(PEARSONS_STANDARD_ERRORS, matrix, col, row), 2);
		case 4: return NumberUtils.round(CorrelationProvider.getValue(SPEARMANS, matrix, col, row), 2);
		}

		return "";
	}

	private static String getFeatureLabels(List<? extends IFeature> features) {
		if (features == null || features.size() < 2) return "<No Feature>/<No Feature>";
		return features.get(0).getDisplayName() + "/" + features.get(1).getDisplayName();
	}

}