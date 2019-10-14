package eu.openanalytics.phaedra.ui.plate.chart.v2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.DataProviderSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.MinMaxFilter;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculation;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.ui.plate.chart.v2.filter.ClassificationFilter;
import eu.openanalytics.phaedra.ui.plate.chart.v2.filter.WellStatusFilter;
import eu.openanalytics.phaedra.ui.plate.chart.v2.filter.WellTypeFilter;

public class WellDataProvider extends JEPAwareDataProvider<Plate, Well> {

	private static final String USE_DEFAULT_NORM = "USE_DEFAULT_NORM";

	private final Supplier<? extends DataUnitConfig> dataUnitSupplier;
	
	private List<Feature> wellFeatures;
	private List<WellProperty> wellProperties;

	private Map<String, List<String>> stringPropertyValues;
	private Map<Plate, List<Well>> wellsByPlate;

	private boolean useDefaultNormalization;
	
	
	public WellDataProvider(final Supplier<? extends DataUnitConfig> dataUnitSupplier) {
		this.dataUnitSupplier = dataUnitSupplier;
	}
	
	
	@Override
	public void initialize() {
		super.initialize();
		wellFeatures = new ArrayList<>();
		wellProperties = Collections.emptyList();
		stringPropertyValues = new HashMap<>();
		wellsByPlate = new HashMap<>();
	}

	public boolean isUseDefaultNormalization() {
		return useDefaultNormalization;
	}

	public void setUseDefaultNormalization(boolean useDefaultNormalization) {
		this.useDefaultNormalization = useDefaultNormalization;
	}

	@Override
	public List<IFilter<Plate, Well>> createFilters() {
		List<IFilter<Plate, Well>> filters = new ArrayList<IFilter<Plate, Well>>();
		filters.add(new MinMaxFilter<Plate, Well>(getSelectedFeatures().size(), this));
		filters.add(new WellTypeFilter(this));
		filters.add(new WellStatusFilter(this));
		filters.add(new ClassificationFilter(this));
		return filters;
	}

	@Override
	public List<String> loadDataAsList(final List<Well> wells) {
		// Get the Numeric Features.
		List<Feature> newFeatures = PlateUtils.getFeatures(wells.get(0));
		newFeatures = newFeatures.stream().filter(f -> f.isNumeric()).collect(Collectors.toCollection(ArrayList::new));
		Collections.sort(newFeatures, ProtocolUtils.FEATURE_NAME_SORTER);
		List<WellProperty> wellProperties = Arrays.asList(WellProperty.values());

		// Sort the incoming selection.
		Collections.sort(wells, PlateUtils.WELL_EXP_NAME_PLATE_BARCODE_WELL_NR_SORTER);
		List<Plate> plates = new ArrayList<>();
		Map<Plate, List<Well>> newWellsByPlate = new HashMap<>();
		for (Well w : wells) {
			Plate plate = w.getPlate();
			CollectionUtils.addUnique(plates, plate);
			newWellsByPlate.putIfAbsent(plate, new ArrayList<>());
			newWellsByPlate.get(plate).add(w);
		}

		// The amount of rows is identical to the number of wells.
		int rowCount = wells.size();
		Map<Plate, float[][]> newData = new HashMap<>();
		Map<Plate, Integer> newDataSizes = new HashMap<>();

		for (Plate plate : plates) {
			List<Well> wellsFromPlate = newWellsByPlate.get(plate);

			int dataSize = wellsFromPlate.size();
			float[][] data = new float[newFeatures.size() + wellProperties.size() + 1][];

			newData.put(plate, data);
			newDataSizes.put(plate, dataSize);
		}

		this.wellFeatures = newFeatures;
		this.wellProperties = wellProperties;
		stringPropertyValues = new HashMap<>();
		wellsByPlate = newWellsByPlate;
		setCurrentData(newData);
		setDataSizes(newDataSizes);
		setTotalRowCount(rowCount);
		setCurrentEntities(plates);

		List<String> featureNames = new ArrayList<>();
		featureNames.addAll(CollectionUtils.transform(newFeatures, ProtocolUtils.FEATURE_NAMES));
		for (WellProperty prop: wellProperties) featureNames.add(prop.getLabel());
		featureNames.add(Density2DChart.UNIT_WEIGHT);
		featureNames.add(EXPRESSIONSTRING);
		return featureNames;
	}

	@Override
	public float[] loadFeatureData(int col, Plate plate, IProgressMonitor monitor) {
		int size = getDataSizes().get(plate);
		float[] values = new float[size];
		int wellNr = 0;
		List<Well> wells = wellsByPlate.get(plate);

		if (col < wellFeatures.size()) {
			// It's a Feature column. Retrieve Feature values.
			PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
			Feature f = wellFeatures.get(col);

			for (Well w : wells) {
				// Some calculated Features have to read the HDF5 file which will cause a canceled progress to hang.
				if (monitor.isCanceled()) return null;
				values[wellNr++] = (float) accessor.getNumericValue(w, f, isUseDefaultNormalization() ? f.getNormalization() : null);
			}
			return values;
		} else if (col - this.wellFeatures.size() < this.wellProperties.size()) {
			// It's a Well property. Retrieve Well properties.
			WellProperty prop = this.wellProperties.get(col - wellFeatures.size());
			DataDescription dataDescription = prop.getDataDescription();
			final DataUnitConfig dataUnitConfig = this.dataUnitSupplier.get();
			switch (dataDescription.getDataType()) {
			case Integer:
			case Real:
				for (Well well : wells) values[wellNr++] = (float)prop.getRealValue(well, dataUnitConfig);
				return values;
			default:
				for (Well well : wells) {
					// Convert the String values to a sequence number.
					String value = prop.getStringValue(well);
					stringPropertyValues.putIfAbsent(prop.getLabel(), new ArrayList<>());
					List<String> uniqueValues = stringPropertyValues.get(prop.getLabel());
					CollectionUtils.addUnique(uniqueValues, value);
					values[wellNr++] = uniqueValues.indexOf(value);
				}
				return values;
			}
		} else {
			Arrays.fill(values, 1.0f);
			return values;
		}
	}

	@Override
	public String[] getAxisLabels() {
		String[] labels = super.getAxisLabels();
		for (int i = 0; i < labels.length; i++) {
			int index = getFeatureIndex(labels[i]);
			if (index >= 0) {
				if (index < wellFeatures.size()) {
					if (isUseDefaultNormalization()) {
						labels[i] += " [" + wellFeatures.get(index).getNormalization() + "]";
					}
					continue;
				}
				else if (index - this.wellFeatures.size() < this.wellProperties.size()) {
					WellProperty prop = this.wellProperties.get(index - this.wellFeatures.size());
					DataDescription dataDescription = prop.getDataDescription();
					if (dataDescription.getDataType() == DataType.Real
							&& dataDescription.getContentType() == ContentType.Concentration) {
						ConcentrationUnit outputUnit = this.dataUnitSupplier.get().getConcentrationUnit();
						labels[i] += " [" + outputUnit.getAbbr() + "]";
					}
				}
			}
		}
		return labels;
	}

	@Override
	public String[] getAxisValueLabels(String feature) {
		List<String> labels = stringPropertyValues.get(feature);
		if (labels == null || labels.isEmpty()) return null;
		return labels.toArray(new String[labels.size()]);
	}

	@Override
	public Plate getPlate() {
		return getCurrentEntities().get(0);
	}

	@Override
	public Object getRowObject(long row) {
		Plate plate = getKey(row);
		int offset = getKeyRange(plate)[0];
		int wellNr = (int) (row - offset);
		List<Well> list = wellsByPlate.get(plate);
		if (list == null) return null;
		return list.get(wellNr);
	}

	@Override
	public ISelection createSelection(BitSet selectionBitSet) {
		if (selectionBitSet == null) return null;

		List<Well> selectedWells = new ArrayList<Well>();
		for (int i = selectionBitSet.nextSetBit(0); i >= 0; i = selectionBitSet.nextSetBit(i+1)) {
			selectedWells.add(getCurrentItems().get(i));
		}

		if (selectedWells.isEmpty()) {
			selectedWells.addAll(getCurrentItems());
		}

		return new StructuredSelection(selectedWells);
	}

	@Override
	public BitSet createSelection(List<?> selection) {
		@SuppressWarnings("unchecked")
		List<Well> wells = (List<Well>) selection;

		BitSet selectionBitSet = new BitSet(getTotalRowCount());

		wells.forEach(w -> {
			int indexOf = getCurrentItems().indexOf(w);
			if (indexOf >= 0) selectionBitSet.set(indexOf);
		});

		return selectionBitSet;
	}

	@Override
	public DataProviderSettings<Plate, Well> getDataProviderSettings() {
		DataProviderSettings<Plate, Well> settings = super.getDataProviderSettings();
		settings.setMiscSetting(USE_DEFAULT_NORM, "" + isUseDefaultNormalization());
		return settings;
	}

	@Override
	public void setDataProviderSettings(DataProviderSettings<Plate, Well> settings) {
		super.setDataProviderSettings(settings);
		String usePlateLimits = settings.getMiscSettings().getOrDefault(USE_DEFAULT_NORM, "false");
		setUseDefaultNormalization(Boolean.valueOf(usePlateLimits));
	}

	@Override
	public Map<String, List<String>> getFeaturesPerGroup() {
		Map<String, List<String>> featureGroups = new LinkedHashMap<>();

		List<IFeature> features = new ArrayList<>(wellFeatures);
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
	protected float[] evaluateArray(String expression, Plate plate) throws CalculationException {
		List<Well> wells = wellsByPlate.get(plate);
		float[] results = new float[wells.size()];
		for (int i = 0; i < wells.size(); i++) {
			results[i] = JEPCalculation.evaluate(expression, wells.get(i));
		}
		return results;
	}

}