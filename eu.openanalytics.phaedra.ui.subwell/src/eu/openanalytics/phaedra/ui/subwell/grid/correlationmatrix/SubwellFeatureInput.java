package eu.openanalytics.phaedra.ui.subwell.grid.correlationmatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.BaseFeatureInput;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;

public class SubwellFeatureInput extends BaseFeatureInput<SubWellFeature> {

	public SubwellFeatureInput(List<SubWellFeature> features, List<Well> wells, CorrelationType sortType, int order) {
		super(features, wells, sortType, order);
	}

	@Override
	protected RealMatrix createMatrix() {
		int maxSize = 0;
		Map<SubWellFeature, float[]> dataMap = new HashMap<>();
		for (SubWellFeature f : getSelectedFeatures()) {
			float[] allData = new float[] {};
			for (Well w : getCurrentWells()) {
				float[] data = SubWellService.getInstance().getNumericData(w, f);
				allData = concatinateArray(allData, data);
			}
			if (allData.length > maxSize) {
				maxSize = allData.length;
			}
			dataMap.put(f, allData);
		}

		if (maxSize != 0) {
			RealMatrix matrix = new BlockRealMatrix(maxSize, getSelectedFeatures().size());
			int featureIndex = 0;
			for (SubWellFeature f : getSelectedFeatures()) {
				float[] allData = dataMap.get(f);
				int i = 0;
				for (; i < allData.length; i++) {
					matrix.addToEntry(i, featureIndex, allData[i]);
				}
				for (; i < maxSize; i++) {
					matrix.addToEntry(i, featureIndex, Double.NaN);
				}
				featureIndex++;
			}
			return matrix;
		}

		return new BlockRealMatrix(10, getSelectedFeatures().size());
	}

	private float[] concatinateArray(float[] a, float[] b) {
		int aLen = a.length;
		if (b == null) {
			return a;
		}
		int bLen = b.length;
		float[] c = new float[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

}