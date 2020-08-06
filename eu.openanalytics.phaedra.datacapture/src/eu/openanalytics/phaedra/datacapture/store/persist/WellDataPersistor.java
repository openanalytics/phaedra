package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;
import java.util.List;

import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.DefaultDataCaptureStore;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellDataPersistor extends BaseDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		// Prevent JPA deadlock: loop through all wells and features inside a lock to ensure they are cached.
		GenericEntityService.getInstance().runInLock(() -> {
			for (Well w: plate.getWells()) w.getWellType();
			for (Feature f: PlateUtils.getFeatures(plate)) f.getDisplayName();
			return null;
		});
		
		// Retrieve existing well data, if any.
		List<FeatureValue> existingData = PlateService.getInstance().getWellData(plate);
		
		for (String featureName: getNames(store, DefaultDataCaptureStore.WELL_DATA_PREFIX)) {
			Object data = store.readValue(DefaultDataCaptureStore.WELL_DATA_PREFIX + featureName);
			FeatureValueGrid dataGrid = createValueGrid(plate, featureName, data, existingData);
			
			if (dataGrid == null) continue;
			if (dataGrid.feature.isNumeric()) {
				PlateService.getInstance().updateWellDataRaw(plate, dataGrid.feature, dataGrid.getNumericValues(), existingData.isEmpty());
			} else {
				PlateService.getInstance().updateWellDataRaw(plate, dataGrid.feature, dataGrid.getStringValues(), existingData.isEmpty());
			}
		}
		
		// Reset the data accessor for this plate, since the raw data just changed.
		CalculationService.getInstance().getAccessor(plate).reset();
	}
	
	private FeatureValueGrid createValueGrid(Plate plate, String featureName, Object data, List<FeatureValue> existingData) {
		Feature feature = ProtocolUtils.getFeatureByName(featureName, ProtocolUtils.getProtocolClass(plate));
		if (feature == null) return null;
		
		FeatureValueGrid grid = new FeatureValueGrid(plate, feature, existingData);
		
		if (data instanceof float[]) {
			float[] numData = (float[]) data;
			for (int i=0; i<numData.length; i++) {
				FeatureValue v = getFeatureValue(grid, i);
				if (v != null) v.setRawNumericValue(numData[i]);
			}
		} else if (data instanceof String[]) {
			String[] strData = (String[]) data;
			for (int i=0; i<strData.length; i++) {
				FeatureValue v = getFeatureValue(grid, i);
				if (v != null) v.setRawStringValue(strData[i]);
			}
		}
		
		return grid;
	}

	private FeatureValue getFeatureValue(FeatureValueGrid grid, int index) {
		int[] pos = NumberUtils.getWellPosition(index+1, grid.plate.getColumns());
		if (pos[0] > grid.plate.getRows() || pos[1] > grid.plate.getColumns()) return null;
		
		FeatureValue v = grid.get(pos[0], pos[1]);
		if (v == null) {
			v = new FeatureValue();
			v.setWell(PlateUtils.getWell(grid.plate, pos[0], pos[1]));
			v.setFeature(grid.feature);
			grid.addValue(pos[0], pos[1], v);
		}
		
		return v;
	}
	
	private static class FeatureValueGrid {

		private Plate plate;
		private Feature feature;
		
		private FeatureValue[][] grid;
		
		public FeatureValueGrid(Plate plate, Feature feature, List<FeatureValue> values) {
			this.plate = plate;
			this.feature = feature;
			this.grid = new FeatureValue[plate.getRows()][plate.getColumns()];
			for (FeatureValue value: values) {
				if (value.getFeature() != feature) continue;
				Well well = value.getWell();
				int row = well.getRow();
				int col = well.getColumn();
				grid[row-1][col-1] = value;
			}
		}

		public void addValue(int row, int column, FeatureValue value) {
			grid[row-1][column-1] = value;
		}

		public FeatureValue get(int row, int column) {
			return grid[row-1][column-1];
		}

		public double[] getNumericValues() {
			double[] values = new double[PlateUtils.getWellCount(plate)];
			for (int i=0; i<values.length; i++) {
				int[] pos = NumberUtils.getWellPosition(i+1, plate.getColumns());
				FeatureValue fv = grid[pos[0]-1][pos[1]-1];
				values[i] = (fv == null) ? Double.NaN : fv.getRawNumericValue();
			}
			return values;
		}
		
		public String[] getStringValues() {
			String[] values = new String[PlateUtils.getWellCount(plate)];
			for (int i=0; i<values.length; i++) {
				int[] pos = NumberUtils.getWellPosition(i+1, plate.getColumns());
				FeatureValue fv = grid[pos[0]-1][pos[1]-1];
				values[i] = (fv == null) ? null : fv.getRawStringValue();
			}
			return values;
		}
	}
}
