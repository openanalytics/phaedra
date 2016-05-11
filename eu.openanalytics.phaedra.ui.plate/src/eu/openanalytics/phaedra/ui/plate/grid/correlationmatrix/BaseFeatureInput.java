package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.eclipse.swt.SWT;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;

public abstract class BaseFeatureInput<FEATURE extends IFeature> {

	private List<FEATURE> selectedFeatures;
	private List<Well> currentWells;

	private RealMatrix matrix;

	public BaseFeatureInput(List<FEATURE> features, List<Well> wells, CorrelationType sortType, int order) {
		this.selectedFeatures = features;
		this.currentWells = wells;

		createMatrix(sortType, order);
	}

	public List<FEATURE> getSelectedFeatures() {
		return selectedFeatures;
	}

	public List<Well> getCurrentWells() {
		return currentWells;
	}

	public RealMatrix getMatrix() {
		return matrix;
	}

	protected abstract RealMatrix createMatrix();

	/**
	 * Create the Matrix. Optional sortType and order.
	 *
	 * @param sortType The correlation type on which should be sorted. Can be 'null'.
	 * @param order The sorting order. SWT.UP or SWT.DOWN
	 */
	private void createMatrix(CorrelationType sortType, int order) {
		matrix = createMatrix();

		// Sort if needed.
		if (sortType != null) {
			int size = selectedFeatures.size();
			Map<FEATURE, Integer> featureSortMap = new HashMap<>();
			for (int i = 0; i < size; i++) {
				featureSortMap.put(selectedFeatures.get(i), i);
			}

			// Get the absolute mean total for each column.
			double[] colMeans = new double[size];
			for (int col = 0; col < size; col++) {
				double colTotalAbs = 0d;
				for (int row = 0; row < size; row++) {
					double d = CorrelationProvider.getValue(sortType, matrix, col, row);
					if (!Double.isNaN(d) && !Double.isInfinite(d)) colTotalAbs += Math.abs(d);
				}
				colMeans[col] = colTotalAbs / size;
			}

			// Sort the Features using the absolute mean total array.
			Collections.sort(selectedFeatures, (f1, f2) -> {
				int i1 = featureSortMap.get(f1);
				int i2 = featureSortMap.get(f2);
				int compare = Double.compare(colMeans[i1], colMeans[i2]);
				return order == SWT.UP ? -compare : compare;
			});

			// Sort the Matrix as well.
			RealMatrix matrixSorted = new BlockRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());
			for (int col = 0; col < size && col < matrix.getColumnDimension(); col++) {
				FEATURE f = selectedFeatures.get(col);
				matrixSorted.setColumn(col, matrix.getColumn(featureSortMap.get(f)));
			}
			matrix = matrixSorted;
		}
	}

}
