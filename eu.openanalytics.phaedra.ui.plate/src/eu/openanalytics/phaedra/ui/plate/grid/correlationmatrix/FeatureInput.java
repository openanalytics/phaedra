package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import java.util.List;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;

public class FeatureInput extends BaseFeatureInput<Feature> {

	public FeatureInput(List<Feature> features, List<Well> wells, CorrelationType sortType, int order) {
		super(features, wells, sortType, order);
	}

	@Override
	protected RealMatrix createMatrix() {
		RealMatrix matrix = new BlockRealMatrix(getCurrentWells().size(), getSelectedFeatures().size());

		int featureIndex = 0;
		for (Feature f : getSelectedFeatures()) {
			int wellIndex = 0;
			for (Well w : getCurrentWells()) {
				PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(w.getPlate());
				double d = accessor.getNumericValue(w, f, f.getNormalization());
				if (!Double.isNaN(d) && !Double.isInfinite(d)) matrix.addToEntry(wellIndex, featureIndex, d);
				wellIndex++;
			}
			featureIndex++;
		}

		return matrix;
	}

}