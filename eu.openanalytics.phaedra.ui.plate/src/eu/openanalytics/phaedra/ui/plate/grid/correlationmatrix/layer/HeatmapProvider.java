package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer;

import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS_STANDARD_ERRORS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.SPEARMANS;

import org.apache.commons.math3.linear.RealMatrix;

public class HeatmapProvider {

	public static double getValue(int type, RealMatrix matrix, int col, int row) {
		if (matrix == null) return 0d;

		switch (type) {
		case 0: return CorrelationProvider.getValue(PEARSONS, matrix, col, row);
		case 1: return CorrelationProvider.getValue(PEARSONS_STANDARD_ERRORS, matrix, col, row);
		case 2: return CorrelationProvider.getValue(SPEARMANS, matrix, col, row);
		}

		return 0d;
	}

}