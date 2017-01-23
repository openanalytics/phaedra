package eu.openanalytics.phaedra.link.data.welldata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkException;
import eu.openanalytics.phaedra.link.data.IDataLinkerComponent;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This component links the following items to the target plate:
 * <ul>
 * <li>Well data: all feature values available in the HDF5 file.</li>
 * <li>Well info: optional well info such as description, status, etc.</li>
 * </ul>
 */
public class WellDataLinker implements IDataLinkerComponent {

	private List<FeatureValueGrid> gridsToImport;
	private boolean plateHasWellData;
	private Plate plate;
	
	@Override
	public void prepareLink(PlateReading reading, HDF5File dataFile, Plate destination, IProgressMonitor monitor) throws DataLinkException {
		
		plate = destination;
		gridsToImport = new ArrayList<FeatureValueGrid>();
		
		// Force loop through wells and features inside a lock to prevent JPA deadlock.
		JDBCUtils.lockEntityManager(Screening.getEnvironment().getEntityManager());
		try {
			for (Well w: plate.getWells()) w.getWellType();
			for (Feature f: PlateUtils.getFeatures(plate)) f.getDisplayName();
		} finally {
			JDBCUtils.unlockEntityManager(Screening.getEnvironment().getEntityManager());	
		}
		
		try {
			Map<Feature, FeatureValueGrid> existingGrids = getExistingValueGrids();
			
			List<String> features = dataFile.getWellFeatures();
			for (String featureName: features) {
				String[] stringValues = null;
				float[] numericValues = null;
				if (dataFile.isWellDataNumeric(featureName)) {
					numericValues = dataFile.getNumericWellData(featureName);
				} else {
					stringValues = dataFile.getStringWellData(featureName);
				}
				
				FeatureValueGrid grid = doImportFeature(featureName, stringValues, numericValues, existingGrids);
				if (grid != null) gridsToImport.add(grid);
			}
		} catch (IOException e) {
			throw new DataLinkException("Failed to read well data", e);
		}
	}
	
	@Override
	public void executeLink() throws DataLinkException {
		
		// Make sure the plate exists before creating Compound objects that refer to it.
		boolean newPlate = (plate.getId() == 0);
		if (newPlate) PlateService.getInstance().updatePlate(plate);

		// Save data.
		for (FeatureValueGrid grid: gridsToImport) {
			Feature f = grid.getFeature();
			if (f.isNumeric()) PlateService.getInstance().updateWellDataRaw(plate, f, grid.getNumericValues(), !plateHasWellData);
			else PlateService.getInstance().updateWellDataRaw(plate, f, grid.getStringValues(), !plateHasWellData);
		}
		
		// Reset the data accessor for this plate, since the raw data just changed.
		CalculationService.getInstance().getAccessor(plate).reset();
	}
	
	@Override
	public void rollback() {
		// Nothing to do.
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private FeatureValueGrid doImportFeature(String featureName, String[] stringValues, float[] numericValues, Map<Feature, FeatureValueGrid> existingGrids) {
		Feature feature = getFeature(featureName);
		int colsPerRow = plate.getColumns();
		int size = (stringValues==null) ? numericValues.length : stringValues.length;

		FeatureValueGrid grid = existingGrids.get(feature);
		for (int i=0; i<size; i++) {
			int[] pos = NumberUtils.getWellPosition(i+1, colsPerRow);
			
			if (pos[0] > plate.getRows() || pos[1] > plate.getColumns()) continue;
			
			// Apply values to the feature grid.
			if (grid != null) {
				FeatureValue v = grid.get(pos[0], pos[1]);
				if (v == null) {
					v = new FeatureValue();
					v.setWell(PlateUtils.getWell(plate, pos[0], pos[1]));
					v.setFeature(feature);
					grid.addValue(pos[0], pos[1], v);
				}
				if (stringValues == null) {
					v.setRawNumericValue(numericValues[i]);
				} else {
					v.setRawStringValue(stringValues[i]);
				}
			}
		}
		return grid;
	}

	private Feature getFeature(String featureName) {
		List<Feature> features = PlateUtils.getFeatures(plate);
		Feature feature = null;
		for (Feature f: features) {
			if (f.getName().equalsIgnoreCase(featureName)) {
				feature = f;
				break;
			}
		}
		return feature;
	}

	private Map<Feature, FeatureValueGrid> getExistingValueGrids() {
		Map<Feature, FeatureValueGrid> grids = new HashMap<>();
		
		List<Feature> features = PlateUtils.getFeatures(plate);
		List<FeatureValue> values = PlateService.getInstance().getWellData(plate);
		plateHasWellData = !values.isEmpty();
		for (Feature f: features) {
			FeatureValueGrid grid = new FeatureValueGrid(plate, f, values);
			grids.put(f, grid);
		}
		
		return grids;
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

		public Feature getFeature() {
			return feature;
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
