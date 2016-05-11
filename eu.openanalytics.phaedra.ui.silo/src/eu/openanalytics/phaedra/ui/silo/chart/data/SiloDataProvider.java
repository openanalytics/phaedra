package eu.openanalytics.phaedra.ui.silo.chart.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.silo.SiloConstants;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.BaseDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.MinMaxFilter;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

import eu.openanalytics.phaedra.ui.silo.Activator;

//TODO This is seriously broken for silos with multiple data groups.
public class SiloDataProvider extends BaseDataProvider<Silo, Silo> {

	private static final String SPLITTER = " @ ";

	private Silo silo;
	private ISiloAccessor<PlatformObject> siloAccessor;

	private Map<String, List<String>> dataSets;
	private Map<String, List<String>> stringPropertyValues;

	private String currentDataGroup;

	@Override
	public void initialize() {
		super.initialize();
		dataSets = new HashMap<>();
		stringPropertyValues = new HashMap<>();
	}

	@Override
	public Silo getKey(long row) {
		return silo;
	}

	@Override
	public Object getRowObject(long row) {
		return getWell((int) row);
	}

	@Override
	public ISelection createSelection(BitSet selectionBitSet) {
		if (selectionBitSet == null) return null;

		int siloType = siloAccessor.getSilo().getType();

		if (siloType == GroupType.SUBWELL.getType()) {
			Map<Well, SubWellSelection> wellMap = new HashMap<>();
			float[] indexColumn = getCellIndexColumn();
			for (int i = 0; i < getTotalRowCount(); i++) {
				if (selectionBitSet.get(i)) {
					int indexId = ((Number) indexColumn[i]).intValue();
					Well w = getWell(i);
					if (!wellMap.containsKey(w) ) {
						SubWellSelection subwellSel = new SubWellSelection(w, new BitSet());
						wellMap.put(w, subwellSel);
					}
					wellMap.get(w).getIndices().set(indexId);
				}
			}

			List<SubWellSelection> subwells = new ArrayList<>();
			for (SubWellSelection selection : wellMap.values()) {
				subwells.add(selection);
			}
			return new StructuredSelection(subwells);
		} else if (siloType == GroupType.WELL.getType()) {
			List<Well> wells = new ArrayList<>();
			for (int i = 0; i < getTotalRowCount(); i++) {
				if (selectionBitSet.get(i)) {
					Well w = getWell(i);
					CollectionUtils.addUnique(wells, w);
				}
			}

			return new StructuredSelection(wells);
		}

		return null;
	}

	@Override
	public BitSet createSelection(List<?> entities) {
		BitSet selectionBitSet = new BitSet(getTotalRowCount());

		if (!entities.isEmpty()) {
			Object o = entities.get(0);

			if (o instanceof SubWellSelection) {
				@SuppressWarnings("unchecked")
				List<SubWellSelection> subwells = (List<SubWellSelection>) entities;

				float[] wellIDColumn = getWellIDColumn();
				float[] cellIndexColumn = getCellIndexColumn();
				if (wellIDColumn != null) {
					for (int i = 0; i < getTotalRowCount(); i++) {
						long wellID = (long) wellIDColumn[i];
						if (wellID == 0) continue;
						for (SubWellSelection swSel : subwells) {
							if (swSel.getWell().getId() == wellID) {
								if (cellIndexColumn != null) {
									int cellIndex = (int) cellIndexColumn[i];
									if (swSel.getIndices().get(cellIndex)) selectionBitSet.set(i);
								} else {
									selectionBitSet.set(i);
								}
							}
						}
					}
				}
			}

			if (o instanceof Well) {
				@SuppressWarnings("unchecked")
				List<Well> wells = (List<Well>) entities;

				float[] wellIDColumn = getWellIDColumn();
				if (wellIDColumn != null) {
					for (int i = 0; i < getTotalRowCount(); i++) {
						long wellID = (long) wellIDColumn[i];
						if (wellID == 0) continue;
						for (Well well : wells) {
							if (well.getId() == wellID) selectionBitSet.set(i);
						}
					}
				}
			}
		}

		return selectionBitSet;
	}

	@Override
	public void setSelectedFeature(String feature, int axis, IProgressMonitor monitor) {
		super.setSelectedFeature(feature, axis, monitor);
		loadDataGroup();
	}

	@Override
	public void setSelectedFeatures(List<String> selectedFeatures, IProgressMonitor monitor) {
		super.setSelectedFeatures(selectedFeatures, monitor);
		loadDataGroup();
	}

	@Override
	public Map<String, List<String>> getFeaturesPerGroup() {
		return dataSets;
	}

	@Override
	public List<IFilter<Silo, Silo>> createFilters() {
		List<IFilter<Silo, Silo>> filters = new ArrayList<IFilter<Silo, Silo>>();
		filters.add(new MinMaxFilter<Silo, Silo>(getSelectedFeatures().size(), this));
		return filters;
	}

	@Override
	public List<String> loadDataAsList(List<Silo> silos) {
		silo = silos.get(0);
		siloAccessor = SiloService.getInstance().getSiloAccessor(silo);
		List<String> featureNames = new ArrayList<>();
		Map<String, List<String>> newDataSets = new HashMap<>();

		try {
			int totalDataSize = 0;

			// Loop the datasets.
			String[] dataGroups = siloAccessor.getDataGroups();
			for (String dataGroup : dataGroups) {
				int dataSize = siloAccessor.getRowCount(dataGroup);
				int currentColSize = featureNames.size();

				String[] columns = siloAccessor.getColumns(dataGroup);
				for (int j = 0; j < columns.length; j++) {
					String fieldName = columns[j];
					String key = fieldName + SPLITTER + dataGroup;
					featureNames.add(key);
				}

				totalDataSize = Math.max(dataSize, totalDataSize);
				newDataSets.put(dataGroup, new ArrayList<String>());
				newDataSets.get(dataGroup).addAll(featureNames.subList(currentColSize, featureNames.size()));
			}

			// Unit weight
			featureNames.add(Density2DChart.UNIT_WEIGHT);

			Map<Silo, Integer> newDataSizes = new HashMap<Silo, Integer>();
			newDataSizes.put(silo, totalDataSize);
			Map<Silo, float[][]> newData = new HashMap<>();
			newData.put(silo, new float[featureNames.size()][]);

			dataSets = newDataSets;
			stringPropertyValues = new HashMap<>();
			setCurrentData(newData);
			setDataSizes(newDataSizes);
			setTotalRowCount(totalDataSize);
			setCurrentEntities(silos);
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}

		return featureNames;
	}

	@Override
	public float[] loadFeatureData(int col, Silo silo, IProgressMonitor monitor) {
		int size = getDataSizes().get(silo);
		float[] filteredColumns = new float[size];
		if (col < getFeatures().size()) {
			Arrays.fill(filteredColumns, Float.NaN);
			try {
				String selectedFeature = getFeatures().get(col);
				String dataGroup = getDataGroup(selectedFeature);
				int column = dataSets.get(dataGroup).indexOf(selectedFeature);

				SiloDataType dataType = siloAccessor.getDataType(dataGroup, column);
				switch (dataType) {
				case Double:
					double[] doubleValues = siloAccessor.getDoubleValues(dataGroup, column);
					for (int i = 0; i < filteredColumns.length && i < doubleValues.length; i++) {
						filteredColumns[i] = (float)doubleValues[i];
					}
					break;
				case Float:
					float[] floatValues = siloAccessor.getFloatValues(dataGroup, column);
					for (int i = 0; i < filteredColumns.length && i < floatValues.length; i++) {
						filteredColumns[i] = floatValues[i];
					}
					break;
				case Integer:
					int[] intValues = siloAccessor.getIntValues(dataGroup, column);
					for (int i = 0; i < filteredColumns.length && i < intValues.length; i++) {
						filteredColumns[i] = intValues[i];
					}
					break;
				case Long:
					long[] longValues = siloAccessor.getLongValues(dataGroup, column);
					for (int i = 0; i < filteredColumns.length && i < longValues.length; i++) {
						filteredColumns[i] = longValues[i];
					}
					break;
				case String:
					// Convert the String values to a sequence number.
					stringPropertyValues.putIfAbsent(selectedFeature, new ArrayList<>());
					List<String> uniqueValues = stringPropertyValues.get(selectedFeature);

					String[] data = siloAccessor.getStringValues(dataGroup, column);
					for (int i = 0; i < filteredColumns.length && i < data.length; i++) {
						if (monitor.isCanceled()) return filteredColumns;
						String value = data[i];
						CollectionUtils.addUnique(uniqueValues, value);
						filteredColumns[i] = uniqueValues.indexOf(value);
					}
					break;
				default:
					break;
				}
			} catch (SiloException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
		} else {
			// Last column = Unit Weight
			Arrays.fill(filteredColumns, 1f);
		}
		return filteredColumns;
	};

	@Override
	public String[] getAxisValueLabels(String feature) {
		List<String> labels = stringPropertyValues.get(feature);
		if (labels == null || labels.isEmpty()) return null;
		return labels.toArray(new String[labels.size()]);
	}

	public void removeFeature(String feature) {
		getSelectedFeatures().remove(feature);
		// Reset layer
		setDataBounds(null);
	}

	public float[] getCellIndexColumn() {
		int col = findColumn(SiloConstants.INDEX_COL);
		if (col > -1) loadFeature(SiloConstants.INDEX_COL + SPLITTER + currentDataGroup, new NullProgressMonitor());
		return getColumnData(col, 0);
	}

	public float[] getWellIDColumn() {
		int col = findColumn(SiloConstants.WELL_COL);
		// Make sure the column is loaded.
		if (col > -1) loadFeature(SiloConstants.WELL_COL + SPLITTER + currentDataGroup, new NullProgressMonitor());
		return getColumnData(col, 0);
	}

	public float[] getPlateIDColumn() {
		int col = findColumn(SiloConstants.PLATE_COL);
		if (col < 0) {
			// No Plate column present, use well ID column.
			float[] wellIDs = getWellIDColumn();
			float[] plateIDs = new float[wellIDs.length];
			for (int i = 0; i < wellIDs.length; i++) {
				Well w = getWell(i);
				if (w != null) plateIDs[i] = w.getPlate().getId();
			}
			return plateIDs;
		} else {
			// Make sure the column is loaded.
			loadFeature(SiloConstants.WELL_COL + SPLITTER + currentDataGroup, new NullProgressMonitor());
		}
		return getColumnData(col, 0);
	}

	public String[] getWellTypeColumn() {
		int col = findColumn(SiloConstants.WELL_TYPE_COL);
		if (col > -1) {
			// Make sure the column is loaded.
			loadFeature(SiloConstants.WELL_TYPE_COL + SPLITTER + currentDataGroup, new NullProgressMonitor());
		} else {
			float[] wellIDs = getWellIDColumn();
			String[] wellTypes = new String[wellIDs.length];
			for (int i = 0; i < wellIDs.length; i++) {
				Well w = getWell(i);
				if (w != null) wellTypes[i] = w.getWellType();
			}
			return wellTypes;
		}
		return getColumnAsString(getFeatures().get(col));
	}

	public String[] getColumnAsString(String columnName) {
		int col = getFeatureIndex(columnName);
		if (col > -1) loadFeature(columnName, new NullProgressMonitor());
		float[] columnData = getColumnData(col, 0);
		String[] columnStringData = new String[columnData.length];

		List<String> uniqueValues = stringPropertyValues.get(columnName);
		if (uniqueValues == null) {
			for (int i = 0; i < columnData.length; i++) {
				columnStringData[i] = columnData[i] + "";
			}
		} else {
			for (int i = 0; i < columnData.length; i++) {
				columnStringData[i] = uniqueValues.get((int) columnData[i]);
			}
		}

		return columnStringData;
	}

	private Well getWell(int rowIndex) {
		try {
			String dataGroup = getDataGroup(getSelectedFeature(0));
			return (Well) siloAccessor.getRow(dataGroup, rowIndex).getAdapter(Well.class);
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

	private String getDataGroup(String featureName) {
		String[] split = featureName.split(SPLITTER);
		String dataGroup = split[1];
		return dataGroup;
	}

	private int findColumn(String name) {
		List<String> features = getFeatures();
		for (int i=0; i < features.size(); i++) {
			if (features.get(i).equals(name + SPLITTER + currentDataGroup)) return i;
		}
		return -1;
	}

	private void loadDataGroup() {
		List<String> selectedFeatures = getSelectedFeatures();
		if (selectedFeatures == null || selectedFeatures.isEmpty()) return;
		String newDataGroup = getDataGroup(selectedFeatures.get(0));
		if (currentDataGroup != null && currentDataGroup.equals(newDataGroup)) return;
		currentDataGroup = newDataGroup;
	}
}