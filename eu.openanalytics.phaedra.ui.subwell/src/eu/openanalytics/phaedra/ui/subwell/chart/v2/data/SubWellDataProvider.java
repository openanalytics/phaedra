package eu.openanalytics.phaedra.ui.subwell.chart.v2.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.DataProviderSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.MinMaxFilter;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculation;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.JEPAwareDataProvider;
import eu.openanalytics.phaedra.ui.subwell.Activator;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.filter.ClassificationFilter;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.filter.WellStatusFilter;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.filter.WellTypeFilter;

public class SubWellDataProvider extends JEPAwareDataProvider<Well, Well> {

	private static final String MAX = "max";
	private static final String MIN = "min";
	private static final String USE_PLATE_LIMITS = "USE_PLATE_LIMITS";

	private List<SubWellFeature> subWellFeatures;

	private Map<String, List<String>> stringPropertyValues;

	private boolean usePlateLimits;

	@Override
	public void initialize() {
		super.initialize();
		subWellFeatures = new ArrayList<SubWellFeature>();
		stringPropertyValues = new HashMap<>();
	}

	public boolean isUsePlateLimits() {
		return usePlateLimits;
	}

	public void setUsePlateLimits(boolean usePlateLimits) {
		this.usePlateLimits = usePlateLimits;
	}

	@Override
	public List<IFilter<Well, Well>> createFilters() {
		List<IFilter<Well, Well>> filters = new ArrayList<IFilter<Well, Well>>();
		filters.add(new MinMaxFilter<Well, Well>(getSelectedFeatures().size(), this));
		filters.add(new WellTypeFilter(this));
		filters.add(new WellStatusFilter(this));
		filters.add(new ClassificationFilter(this));
		return filters;
	}

	@Override
	public List<String> loadDataAsList(List<Well> wells) {
		// Get the Numeric Features.
		List<SubWellFeature> newFeatures = PlateUtils.getSubWellFeatures(wells.get(0));
		newFeatures = newFeatures.stream().filter(f -> f.isNumeric()).collect(Collectors.toCollection(ArrayList::new));
		Collections.sort(newFeatures, ProtocolUtils.FEATURE_NAME_SORTER);
		WellProperty[] wellProperties = WellProperty.values();

		int rowCount = 0;
		Map<Well, float[][]> newData = new HashMap<>();
		Map<Well, Integer> newDataSizes = new HashMap<>();

		// Columns 0 -> x: subwell features
		// Columns x+1 -> y: well properties
		// Column y+1: Unit Weight

		for (Well well : wells) {
			float[][] data = new float[newFeatures.size() + wellProperties.length + 1][];

			newData.put(well, data);
			newDataSizes.put(well, 0);
		}

		subWellFeatures = newFeatures;
		stringPropertyValues = new HashMap<>();
		setCurrentData(newData);
		setDataSizes(newDataSizes);
		setTotalRowCount(rowCount);
		setCurrentEntities(wells);

		List<String> featureNames = new ArrayList<>();
		featureNames.addAll(CollectionUtils.transform(newFeatures, ProtocolUtils.FEATURE_NAMES));
		for (WellProperty prop : wellProperties) featureNames.add(prop.getLabel());
		featureNames.add(Density2DChart.UNIT_WEIGHT);
		featureNames.add(EXPRESSIONSTRING);
		return featureNames;
	}

	@Override
	public void loadFeature(String feature, IProgressMonitor monitor) {
		if (feature.equalsIgnoreCase(EXPRESSIONSTRING)) {
			int totalRowCount = getTotalRowCount();
			if (totalRowCount == 0) {
				// The sizes have not yet been retrieved.
				SubWellService.getInstance().preloadData(getCurrentEntities(), subWellFeatures, monitor);
				for (Well w : getCurrentEntities()) {
					int size = SubWellService.getInstance().getNumberOfCells(w);
					totalRowCount += size;
					getDataSizes().put(w, size);
				}
				setTotalRowCount(totalRowCount);
				performFiltering();
			}
		}
		super.loadFeature(feature, monitor);
	}

	@Override
	public float[] loadFeatureData(int col, Well well, IProgressMonitor monitor) {
		int size = getDataSizes().get(well);
		float[] values = new float[size];

		if (col < subWellFeatures.size()) {
			// It's a Feature column. Retrieve Feature values.
			SubWellFeature feature = subWellFeatures.get(col);
			values = SubWellService.getInstance().getNumericData(well, feature, 0, false);

			if (values != null) {
				// Check if this Feature has more data than previous read Features.
				int newSize = values.length;
				if (newSize > size) {
					// This well just got bigger, update total row count and individual size of this well.
					int rowCount = getTotalRowCount();
					rowCount += newSize - size;
					setTotalRowCount(rowCount);
					getDataSizes().put(well, newSize);
					if (getFilters() != null) performFiltering();
				}
			}
		} else if (col - subWellFeatures.size() < WellProperty.values().length) {
			// It's a Well property. Retrieve Well properties.
			WellProperty prop = WellProperty.values()[col - subWellFeatures.size()];
			if (prop.isNumeric()) {
				Arrays.fill(values, (float) prop.getValue(well));
			} else {
				// Convert the String values to a sequence number.
				String value = prop.getStringValue(well);
				stringPropertyValues.putIfAbsent(prop.getLabel(), new ArrayList<>());
				List<String> uniqueValues = stringPropertyValues.get(prop.getLabel());
				CollectionUtils.addUnique(uniqueValues, value);
				Arrays.fill(values, uniqueValues.indexOf(value));
			}
		} else {
			Arrays.fill(values, 1.0f);
		}
		return values;
	}

	@Override
	public void setSelectedFeatures(List<String> selectedFeatures, IProgressMonitor monitor) {
		preloadData(selectedFeatures, monitor);
		super.setSelectedFeatures(selectedFeatures, monitor);
	}

	@Override
	public String[] getAxisValueLabels(String feature) {
		List<String> labels = stringPropertyValues.get(feature);
		if (labels == null || labels.isEmpty())
			return null;
		return labels.toArray(new String[labels.size()]);
	}

	@Override
	public Plate getPlate() {
		return getCurrentEntities().get(0).getPlate();
	}

	@Override
	public Object getRowObject(long row) {
		Well well = getKey(row);
		int offset = getKeyRange(well)[0];
		BitSet bitSet = new BitSet();
		bitSet.set((int) (row - offset));
		return new SubWellSelection(well, bitSet);
	}

	@Override
	public ISelection createSelection(BitSet selectionBitSet) {
		Well currentWell = getKey(0);
		if (currentWell == null) {
			return null;
		}

		List<SubWellSelection> selectedSubWells = new ArrayList<SubWellSelection>();
		BitSet wellSelectionSet = new BitSet();
		if (selectionBitSet != null) {
			int currentSize = getDataSizes().get(currentWell) != null ? getDataSizes().get(currentWell) : 0;
			int offset = 0;
			for (int fullIndex = 0; fullIndex < getTotalRowCount(); fullIndex++) {
				if (fullIndex - offset >= currentSize) {
					// Proceed to next well.
					if (wellSelectionSet.cardinality() > 0) {
						SubWellSelection sel = new SubWellSelection(currentWell, wellSelectionSet);
						selectedSubWells.add(sel);
					}

					wellSelectionSet = new BitSet();
					currentWell = getKey(fullIndex);
					offset += currentSize;
					currentSize = getDataSizes().get(currentWell) != null ? getDataSizes().get(currentWell) : 0;
				}

				if (selectionBitSet.get(fullIndex)) {
					wellSelectionSet.set(fullIndex - offset);
				}
			}

			if (getTotalRowCount() > 0) {
				// Last well
				if (wellSelectionSet.cardinality() > 0) {
					SubWellSelection sel = new SubWellSelection(currentWell, wellSelectionSet);
					selectedSubWells.add(sel);
				}
			}
		}

		if (selectedSubWells.isEmpty()) {
			wellSelectionSet = new BitSet();
			for (Well w : getCurrentEntities()) {
				selectedSubWells.add(new SubWellSelection(w, wellSelectionSet));
			}
		}

		return new StructuredSelection(selectedSubWells);
	}

	@Override
	public BitSet createSelection(List<?> selection) {
		BitSet selectionBitSet = new BitSet(getTotalRowCount());

		if (!selection.isEmpty()) {
			Object o = selection.get(0);
			if (o instanceof Well) {
				@SuppressWarnings("unchecked")
				List<Well> wells = (List<Well>) selection;

				BitSet currentFilter = getCurrentFilter();
				for (int i = 0; i < getTotalRowCount(); i++) {
					if (!currentFilter.get(i)) continue;
					Well w = getKey(i);
					if (w == null || !wells.contains(w)) continue;

					int[] range = getKeyRange(w);
					if (range != null) {
						int to = range[1];
						for (; i <= to; i++) {
							if (!currentFilter.get(i)) continue;
							selectionBitSet.set(i);
						}
						i = to;
					}
				}
			} else if (o instanceof SubWellSelection) {
				Map<Well, BitSet> map = new HashMap<>();
				@SuppressWarnings("unchecked")
				List<SubWellSelection> subWellSelections = (List<SubWellSelection>) selection;
				for (SubWellSelection s : subWellSelections) {
					if (s.getIndices().cardinality() > 0) {
						map.put(s.getWell(), s.getIndices());
					}
				}

				BitSet currentFilter = getCurrentFilter();
				for (int i = 0; i < getTotalRowCount(); i++) {
					if (!currentFilter.get(i)) continue;
					Well w = getKey(i);
					if (w == null || !map.containsKey(w)) continue;

					int[] range = getKeyRange(w);
					if (range != null) {
						int from = range[0];
						int to = range[1];
						BitSet bs = map.get(w);
						for (; i <= to; i++) {
							if (!currentFilter.get(i)) continue;
							if (bs.get(i - from)) selectionBitSet.set(i);
						}
						i = to;
					}
				}
			}
		}

		return selectionBitSet;
	}

	@Override
	public String[] getGates() {
		if (getCurrentEntities() == null || getCurrentEntities().isEmpty()) {
			return new String[0];
		}

		// TODO: What with charts spanning multiple plates? This doesn't seem to be correct.
		Plate plate = getPlate();
		String hdf5Path = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
		try {
			if (!Screening.getEnvironment().getFileServer().exists(hdf5Path)) return new String[0];
		} catch (IOException e) {
			return new String[0];
		}

		try (HDF5File file = HDF5File.openForRead(hdf5Path)) {
			String[] gateData = new String[0];
			if (file.exists(HDF5File.getExtraDataPath() + HDF5File.getGatesPath())) {
				gateData = file.getChildren(HDF5File.getExtraDataPath() + HDF5File.getGatesPath());
			}

			if (gateData != null && gateData.length > 0) {
				String[] fileContents;
				int iterator = 0;
				if (gateData[0].endsWith(".xml")) {
					fileContents = new String[gateData.length];
					for (String data : gateData) {
						if (file.exists(HDF5File.getExtraDataPath() + HDF5File.getGatesPath() + "/" + data)) {
							InputStream stream = file.getExtraData(HDF5File.getGatesPath() + "/" + data);
							fileContents[iterator++] = new String(StreamUtils.readAll(stream));
						}
					}
				} else {
					fileContents = new String[gateData.length];
					for (String data : gateData) {
						// TODO: Currently the first available Well is used. Allow user to chose from which Well to load the gate?
						for (Well well : getCurrentEntities()) {
							if (file.exists(HDF5File.getExtraDataPath() + HDF5File.getGatesPath() + "/" + data + "/"
									+ NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns())
									+ ".xml")) {
								InputStream stream = file.getExtraData(HDF5File.getGatesPath() + "/" + data + "/"
										+ NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns())
										+ ".xml");
								fileContents[iterator++] = new String(StreamUtils.readAll(stream));
								break;
							}
						}
					}
				}
				return fileContents;
			} else {
				return new String[0];
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load gates", e);
		}
	}

	@Override
	public double[] calculateDataBoundsForDimension(int dim) {

		if (!isUsePlateLimits()) return super.calculateDataBoundsForDimension(dim);
		
		double[] bounds = new double[2];
		Plate plate = getPlate();
		
		if (hasJepExpression(dim)) {
			// Calculate via jep
			String feature = getJepExpressions()[dim];
			try {
				bounds[0] = evaluatePlateStat(feature, plate, MIN);
				bounds[1] = evaluatePlateStat(feature, plate, MAX);
			} catch (Exception e) {
				EclipseLog.error("Failed to evaluate plate statistic for " + plate, e, Activator.getDefault());
			}
		} else {
			String featureName = getSelectedFeature(dim);
			SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(featureName, PlateUtils.getProtocolClass(plate));
			if (feature == null) {
				WellProperty prop = WellProperty.getByName(featureName);
				if (prop != null && prop.isNumeric()) {
					for (Well well : plate.getWells()) {
						float value = (float) prop.getValue(well);
						bounds[0] = Math.min(bounds[0], value);
						bounds[1] = Math.max(bounds[1], value);
					}
				}
			} else {
				bounds[0] = StatService.getInstance().calculate(MIN, plate, feature, null);
				bounds[1] = StatService.getInstance().calculate(MAX, plate, feature, null);
			}
		}

		// If a MinMaxFilter is present and specifies bounds, use those bounds instead.
		MinMaxFilter<?, ?> filter = Optional.ofNullable(getFilters()).orElse(Collections.emptyList())
				.stream().filter(f -> f instanceof MinMaxFilter).map(f -> (MinMaxFilter<?, ?>) f).findAny().orElse(null);
		if (filter != null) {
			Double[] min = filter.getMin();
			if (dim >= 0 && dim < min.length && min[dim] != null) bounds[0] = min[dim];
			Double[] max = filter.getMax();
			if (dim >= 0 && dim < max.length && max[dim] != null) bounds[1] = max[dim];
		}
		
		return bounds;
	}

	@Override
	public DataProviderSettings<Well, Well> getDataProviderSettings() {
		DataProviderSettings<Well, Well> settings = super.getDataProviderSettings();
		settings.setMiscSetting(USE_PLATE_LIMITS, "" + isUsePlateLimits());
		return settings;
	}

	@Override
	public void setDataProviderSettings(DataProviderSettings<Well, Well> settings) {
		super.setDataProviderSettings(settings);
		String usePlateLimits = settings.getMiscSettings().getOrDefault(USE_PLATE_LIMITS, "false");
		setUsePlateLimits(Boolean.valueOf(usePlateLimits));
	}

	@Override
	public Map<String, List<String>> getFeaturesPerGroup() {
		Map<String, List<String>> featureGroups = new LinkedHashMap<>();

		List<IFeature> features = new ArrayList<>(subWellFeatures);
		Collections.sort(features, (f1, f2) -> {
			FeatureGroup fg1 = f1.getFeatureGroup();
			FeatureGroup fg2 = f2.getFeatureGroup();
			if (fg1 == null && fg2 == null) return f1.getDisplayName().compareToIgnoreCase(f2.getDisplayName());
			if (fg1 == null) return -1;
			if (fg2 == null) return 1;
			return fg1.getName().compareToIgnoreCase(fg2.getName());
		});

		for (IFeature f : features) {
			String groupName = f.getFeatureGroup() != null ? f.getFeatureGroup().getName() : NO_GROUP;
			if (!featureGroups.containsKey(groupName)) {
				featureGroups.put(groupName, new ArrayList<String>());
			}
			featureGroups.get(groupName).add(f.getDisplayName());
		}

		featureGroups.put(OTHER_GROUP, new ArrayList<String>());

		for (WellProperty prop : WellProperty.values()) {
			featureGroups.get(OTHER_GROUP).add(prop.getLabel());
		}

		featureGroups.putIfAbsent(null, new ArrayList<>());
		featureGroups.get(null).add(EXPRESSIONSTRING);

		return featureGroups;
	}

	@Override
	protected float[] evaluateArray(String expression, Well well) throws CalculationException {
		return JEPCalculation.evaluateArray(expression, well);
	}

	private float evaluatePlateStat(String expression, Plate plate, String stat) {
		List<Well> wells = plate.getWells();
		double[] values = new double[wells.size()];
		for (int i = 0; i < wells.size(); i++) {
			Well well = wells.get(i);
			float[] wellValues = JEPCalculation.evaluateArray(expression, well);
			double[] wellValues2 = new double[wellValues.length];
			for (int j = 0; j < wellValues2.length; j++) wellValues2[j] = wellValues[j];
			values[i] = StatService.getInstance().calculate(stat, wellValues2);
		}
		return (float) StatService.getInstance().calculate(stat, values);
	}

	private void preloadData(List<String> selectedFeatures, IProgressMonitor monitor) {
		if (getCurrentEntities() == null || getCurrentEntities().isEmpty()) return;

		// Look up the features to load.
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(getCurrentEntities().get(0));
		List<SubWellFeature> features = selectedFeatures.stream()
				.map(fName -> ProtocolUtils.getSubWellFeatureByName(fName, pClass))
				.filter(f -> f != null)
				.collect(Collectors.toList());

		SubWellService.getInstance().preloadData(getCurrentEntities(), features, new SubProgressMonitor(monitor, selectedFeatures.size()));
	}
}