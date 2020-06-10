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
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;


public class WellDataPersistor extends BaseDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		// Prevent JPA deadlock: loop through all wells and features inside a lock to ensure they are cached.
		final List<Well> wells = GenericEntityService.getInstance().runInLock(() -> {
			final List<Well> wellList = PlateService.streamableList(plate.getWells());
			for (Well w: wellList) w.getWellType();
			for (Feature f: PlateUtils.getFeatures(plate)) f.getDisplayName();
			return wellList;
		});
		wells.sort(PlateUtils.WELL_NR_SORTER);
		
		// Retrieve existing well data, if any.
		List<FeatureValue> existingData = PlateService.getInstance().getWellData(plate);
		
		for (String featureName: getNames(store, DefaultDataCaptureStore.WELL_DATA_PREFIX)) {
			Object data = store.readValue(DefaultDataCaptureStore.WELL_DATA_PREFIX + featureName);
			FeatureValueGrid dataGrid = createValueGrid(plate, wells, featureName, data, existingData);
			
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
	
	private FeatureValueGrid createValueGrid(Plate plate, List<Well> wells, String featureName, Object data, List<FeatureValue> existingData) {
		Feature feature = ProtocolUtils.getFeatureByName(featureName, ProtocolUtils.getProtocolClass(plate));
		if (feature == null) return null;
		
		FeatureValueGrid grid = new FeatureValueGrid(plate, feature, existingData);
		
		if (data instanceof float[]) {
			float[] numData = (float[])data;
			for (int i = 0; i < wells.size() && i < numData.length; i++) {
				final Well well = wells.get(i);
				if (well.getStatus() == WellStatus.REJECTED_DATACAPTURE.getCode()) {
					continue;
				}
				FeatureValue v = getFeatureValue(grid, well);
				if (v != null) v.setRawNumericValue(numData[i]);
			}
		} else if (data instanceof String[]) {
			String[] strData = (String[]) data;
			for (int i = 0; i < wells.size() && i < strData.length; i++) {
				final Well well = wells.get(i);
				if (well.getStatus() == WellStatus.REJECTED_DATACAPTURE.getCode()) {
					continue;
				}
				FeatureValue v = getFeatureValue(grid, well);
				if (v != null) v.setRawStringValue(strData[i]);
			}
		}
		
		return grid;
	}

	private static FeatureValue getFeatureValue(FeatureValueGrid grid, Well well) {
		FeatureValue v = grid.get(well.getRow(), well.getColumn());
		if (v == null) {
			v = new FeatureValue();
			v.setWell(well);
			v.setFeature(grid.feature);
			grid.addValue(well.getRow(), well.getColumn(), v);
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
