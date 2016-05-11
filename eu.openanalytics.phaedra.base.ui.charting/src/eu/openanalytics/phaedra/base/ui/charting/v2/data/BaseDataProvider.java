package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot.Style;

public abstract class BaseDataProvider<ENTITY, ITEM> implements IDataProvider<ENTITY, ITEM> {

	// TODO: Should move to JEPAwareDataProvider.
	public final static String EXPRESSIONSTRING = "Enter expr.";

	private List<String> features;
	private List<String> selectedFeatures;
	private List<String> auxiliaryFeatures;

	private String title;
	private String[] customAxisLabels;

	private List<IFilter<ENTITY, ITEM>> filters;
	private IGroupingStrategy<ENTITY, ITEM> activeGroupingStrategy;
	private double[][] dataBounds;

	private int totalRowCount;
	private BitSet currentFilter;
	private List<ENTITY> currentEntities;
	private List<ITEM> currentItems;
	private Map<ENTITY, float[][]> currentData;
	private Map<ENTITY, Integer> dataSizes;

	private int availableNumberOfPoints;
	private int selectedNumberOfPoints;
	private int visibleNumberOfPoints;

	private IDataCalculator<ENTITY, ITEM> dataCalculator;

	private ValueObservable dataChangedObservable = new ValueObservable();
	private ValueObservable titleChangedObservable = new ValueObservable();
	private ValueObservable labelChangedObservable = new ValueObservable();

	public BaseDataProvider() {
		initialize();
	}

	@Override
	public void initialize() {
		currentEntities = new ArrayList<ENTITY>();
		currentItems = new ArrayList<ITEM>();
		features = new ArrayList<String>();
		selectedFeatures = new ArrayList<String>();
		auxiliaryFeatures = new ArrayList<String>();
		currentData = new HashMap<ENTITY, float[][]>();
		dataSizes = new HashMap<ENTITY, Integer>();
		dataBounds = null;
		currentFilter = new BitSet();
		totalRowCount = 0;
		if (activeGroupingStrategy == null) {
			activeGroupingStrategy = new DefaultGroupingStrategy<ENTITY, ITEM>();
		}
	}

	@Override
	public String[] getGates() {
		return new String[0];
	}

	@Override
	public void loadData(List<ITEM> data, int dimensions) {
		loadData(data, dimensions, new NullProgressMonitor());
	}

	@Override
	public void loadData(List<ITEM> data, int dimensions, IProgressMonitor monitor) {
		List<String> newFeatures = getFeatures();
		// Synchronize on dataProvider here. Same should be done in the highlighting methods.
		synchronized (this) {
			if (data != null && !data.isEmpty()) {
				newFeatures = loadDataAsList(data);
				if (newFeatures != null && !newFeatures.isEmpty()) {
					features = newFeatures;
					currentItems = data;
				} else {
					return;
				}
			}
		}

		List<String> featuresToSelect = new ArrayList<String>();
		List<String> auxFeaturesToSelect = new ArrayList<String>();

		// Preselect the first x features if no selectedFeatures set
		if (newFeatures != null && newFeatures.size() > 1) {
			// validate if features available in dataset
			for (String prevFeature : selectedFeatures) {
				if (newFeatures.contains(prevFeature)) {
					featuresToSelect.add(prevFeature);
				}
			}
			for (String prevFeature : auxiliaryFeatures) {
				if (newFeatures.contains(prevFeature)) {
					auxFeaturesToSelect.add(prevFeature);
				}
			}
			while (auxFeaturesToSelect.size() < auxiliaryFeatures.size()) {
				auxFeaturesToSelect.add(newFeatures.get(auxFeaturesToSelect.size()));
			}

			// Make sure enough Features are selected
			if (dimensions > 0) {
				while (featuresToSelect.size() < dimensions) {
					featuresToSelect.add(newFeatures.get(featuresToSelect.size()));
				}
			} else if (dimensions < 0) {
				// For charts with dynamic dimensions we have a default of 3 selected features
				while (featuresToSelect.size() < 3) {
					featuresToSelect.add(newFeatures.get(featuresToSelect.size()));
				}
			}
		}

		monitor.beginTask("Loading Features", featuresToSelect.size() + auxFeaturesToSelect.size());
		setSelectedFeatures(featuresToSelect, monitor);
		setAuxiliaryFeatures(auxFeaturesToSelect, monitor);
		monitor.done();

		performFiltering();
	}

	@Override
	public void performFiltering() {
		if (filters == null) {
			filters = createFilters();
		}

		// Reset the filter - set all bits to true
		currentFilter = new BitSet(getTotalRowCount());
		currentFilter.set(0, getTotalRowCount());

		// Apply filters, if any.
		if (filters != null) {
			for (IFilter<ENTITY, ITEM> filter : getFilters()) {
				if (filter.isActive()) {
					filter.filter();
				}
			}
		}
	}

	public abstract List<IFilter<ENTITY, ITEM>> createFilters();

	@Override
	public RowSubset[] performGrouping() {
		return getDataCalculator().performGrouping();
	}

	public abstract List<String> loadDataAsList(List<ITEM> data);

	public float[] loadFeatureData(int col, ENTITY entity, IProgressMonitor monitor) {
		float[] featureData = new float[dataSizes.get(entity)];
		Arrays.fill(featureData, Float.NaN);
		return featureData;
	}

	@Override
	public void loadFeature(String feature, IProgressMonitor monitor) {
		monitor.beginTask("Loading feature " + feature, currentEntities.size());
		try {
			int col = getFeatureIndex(feature);
			for (ENTITY e : currentEntities) {
				if (monitor.isCanceled()) return;
				float[][] data = currentData.get(e);
				// If the data is not yet loaded, load it now.
				if (data[col] == null) data[col] = loadFeatureData(col, e, monitor);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public int getFeatureIndex(String feature) {
		for (int j = 0; j < features.size(); j++) {
			if (features.get(j).equals(feature)) {
				return j;
			}
		}
		return 0;
	}

	@Override
	public ColumnInfo getColumnInfo(int col) {
		String feature = getFeatures().get(col);
		return new ColumnInfo(new DefaultValueInfo(feature, Float.class, feature));
	}

	@Override
	public synchronized float[] getColumnData(int col, int axis) {
		float[] filteredColumns = new float[getTotalRowCount()];
		Arrays.fill(filteredColumns, Float.NaN);
		if (col > -1) {
			int totalIndex = 0;
			for (ENTITY e : currentEntities) {
				float[] featureData = currentData.get(e)[col];
				if (featureData != null) {
					for (float featureValue : featureData) {
						if (currentFilter.get(totalIndex)) {
							filteredColumns[totalIndex] = featureValue;
						}
						totalIndex++;
					}
					int[] range = getKeyRange(e);
					totalIndex = range[1] + 1;
				}
			}
		}
		return filteredColumns;
	}

	@Override
	public double[][] getDataBounds() {
		if (dataBounds == null) {
			dataBounds = calculateDatabounds();
		}
		return dataBounds;
	}

	@Override
	public void setDataBounds(double[][] bounds) {
		this.dataBounds = bounds;
	}

	@Override
	public double[][] calculateDatabounds() {
		return getDataCalculator().calculateDataBounds();
	}

	@Override
	public double[] calculateDataBoundsForDimension(int dimension) {
		double[] bounds = new double[2];
		float[] featureValues = getColumnData(getFeatureIndex(dimension), dimension);

		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < featureValues.length; i++) {
			if (!Float.isNaN(featureValues[i]) && !Float.isInfinite(featureValues[i])) {
				min = Math.min(min, featureValues[i]);
				max = Math.max(max, featureValues[i]);
			}
		}
		if (Float.MAX_VALUE == min || Float.isNaN(min)) {
			bounds[0] = 0;
		} else {
			bounds[0] = min;
		}
		if (-Float.MAX_VALUE == max || Float.isNaN(max)) {
			bounds[1] = 0;
		} else {
			bounds[1] = max;
		}

		return bounds;
	}

	@Override
	public String getSelectedFeature(int dimension) {
		if (dimension < getSelectedFeatures().size()) {
			return getSelectedFeatures().get(dimension);
		} else {
			return getAuxiliaryFeatures().get(dimension - getSelectedFeatures().size());
		}
	}

	@Override
	public void setSelectedFeature(String feature, int axis, IProgressMonitor monitor) {
		loadFeature(feature, monitor);
		selectedFeatures.set(axis, feature);
	}

	@Override
	public void setAuxilaryFeature(String feature, int dimension) {
		setAuxilaryFeature(feature, dimension, new NullProgressMonitor());
	}

	@Override
	public void setAuxilaryFeature(String feature, int dimension, IProgressMonitor monitor) {
		loadFeature(feature, monitor);
		int auxDim = dimension - getSelectedFeatures().size();
		if (auxDim < auxiliaryFeatures.size()) {
			auxiliaryFeatures.set(auxDim, feature);
		} else {
			auxiliaryFeatures.add(feature);
		}
		setDataBounds(calculateDatabounds());
	}

	@Override
	public StarTable generateStarTable() {
		return getDataCalculator().generateStarTable();
	}

	@Override
	public int getDimensionCount() {
		return selectedFeatures != null ? selectedFeatures.size() + auxiliaryFeatures.size() : 0;
	}

	/* Getters and setters */

	@Override
	public List<String> getFeatures() {
		return features;
	}

	@Override
	public List<String> getSelectedFeatures() {
		return selectedFeatures;
	}

	@Override
	public void setSelectedFeatures(List<String> selectedFeatures) {
		setSelectedFeatures(selectedFeatures, new NullProgressMonitor());
	}

	@Override
	public void setSelectedFeatures(List<String> selectedFeatures, IProgressMonitor monitor) {
		for (String f : selectedFeatures) loadFeature(f, new SubProgressMonitor(monitor, 1));
		this.selectedFeatures = selectedFeatures;
	}

	@Override
	public List<String> getAuxiliaryFeatures() {
		return auxiliaryFeatures;
	}

	@Override
	public void setAuxiliaryFeatures(List<String> auxiliaryFeatures) {
		setAuxiliaryFeatures(auxiliaryFeatures, new NullProgressMonitor());
	}

	public void setAuxiliaryFeatures(List<String> auxiliaryFeatures, IProgressMonitor monitor) {
		for (String f : auxiliaryFeatures) loadFeature(f, new SubProgressMonitor(monitor, 1));
		this.auxiliaryFeatures = auxiliaryFeatures;
	}

	@Override
	public List<IFilter<ENTITY, ITEM>> getFilters() {
		return filters;
	}

	@Override
	public void setFilters(List<IFilter<ENTITY, ITEM>> filters) {
		this.filters = filters;
	}

	@Override
	public BitSet getCurrentFilter() {
		return currentFilter;
	}

	@Override
	public int getTotalRowCount() {
		return totalRowCount;
	}

	@Override
	public void setTotalRowCount(int totalRowCount) {
		this.totalRowCount = totalRowCount;
	}

	@Override
	public List<ENTITY> getCurrentEntities() {
		return currentEntities;
	}

	protected void setCurrentEntities(List<ENTITY> currentEntities) {
		this.currentEntities = currentEntities;
	}

	@Override
	public List<ITEM> getCurrentItems() {
		return currentItems;
	}

	protected void setCurrentData(Map<ENTITY, float[][]> currentData) {
		this.currentData = currentData;
	}

	@Override
	public IGroupingStrategy<ENTITY, ITEM> getActiveGroupingStrategy() {
		return activeGroupingStrategy;
	}

	@Override
	public void setActiveGroupingStrategy(IGroupingStrategy<ENTITY, ITEM> activeGroupingStrategy) {
		this.activeGroupingStrategy = activeGroupingStrategy;
	}

	@Override
	public Style[] getStyles(ChartSettings settings) {
		return getActiveGroupingStrategy().getStyles(settings);
	}

	@Override
	public String[] getAxisLabels() {
		String[] originalAxisLabels;
		if (getDimensionCount() == 1) {
			originalAxisLabels = new String[] { getSelectedFeature(0), "Frequency" };
		} else {
			originalAxisLabels = new String[getDimensionCount()];
			for (int i = 0; i < originalAxisLabels.length; i++) {
				originalAxisLabels[i] = getSelectedFeature(i);
			}
		}

		if (customAxisLabels != null) {
			for (int i = 0; i < originalAxisLabels.length; i++) {
				if (i < customAxisLabels.length && customAxisLabels[i] != null && !customAxisLabels[i].isEmpty()) {
					originalAxisLabels[i] = customAxisLabels[i];
				}
			}
		}

		return originalAxisLabels;
	}

	@Override
	public ENTITY getKey(long irow) {
		int offset = 0;
		for (ENTITY entity : getCurrentEntities()) {
			int size = dataSizes.get(entity);
			if ((offset += size) > irow) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public int[] getKeyRange(ENTITY key) {
		int offset = 0;
		for (ENTITY entity : getCurrentEntities()) {
			int size = dataSizes.get(entity);

			if (entity.equals(key)) {
				return new int[] { offset, offset + size - 1 };
			} else {
				offset += size;
			}
		}
		return new int[] { 0, 0};
	}

	@Override
	public Map<ENTITY, Integer> getDataSizes() {
		return dataSizes;
	}

	protected void setDataSizes(Map<ENTITY, Integer> dataSizes) {
		this.dataSizes = dataSizes;
	}

	@Override
	public String[] getCustomAxisLabels() {
		return customAxisLabels;
	}

	@Override
	public void setCustomAxisLabels(String[] customAxisLabels) {
		this.customAxisLabels = customAxisLabels;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String[] getAxisValueLabels(String feature) {
		// Default: no specific labels -> the numeric values will be used.
		return null;
	}

	@Override
	public int getAvailableNumberOfPoints() {
		return availableNumberOfPoints;
	}

	@Override
	public void setAvailableNumberOfPoints(int vailableNumberOfPoints) {
		this.availableNumberOfPoints = vailableNumberOfPoints;
	}

	@Override
	public int getSelectedNumberOfPoints() {
		return selectedNumberOfPoints;
	}

	@Override
	public void setSelectedNumberOfPoints(int selectedNumberOfPoints) {
		this.selectedNumberOfPoints = selectedNumberOfPoints;
	}

	@Override
	public int getVisibleNumberOfPoints() {
		return visibleNumberOfPoints;
	}

	@Override
	public void setVisibleNumberOfPoints(int visibleNumberOfPoints) {
		this.visibleNumberOfPoints = visibleNumberOfPoints;
	}

	@Override
	public ValueObservable getDataChangedObservable() {
		return dataChangedObservable;
	}

	@Override
	public ValueObservable getTitleChangedObservable() {
		return titleChangedObservable;
	}

	@Override
	public ValueObservable getLabelChangedObservable() {
		return labelChangedObservable;
	}

	@Override
	public DataProviderSettings<ENTITY, ITEM> getDataProviderSettings() {
		DataProviderSettings<ENTITY, ITEM> settings = new DataProviderSettings<>();
		settings.setFeatures(selectedFeatures);
		settings.setAuxiliaryFeatures(auxiliaryFeatures);
		Map<String, Object> filterProperties = new HashMap<>();
		if (filters != null) {
			for (IFilter<ENTITY, ITEM> filter : filters) {
				filterProperties.put(filter.getClass().getName(), filter.getProperties());
			}
		}
		settings.setFilterProperties(filterProperties);
		settings.setGroupingStrategy(getActiveGroupingStrategy());
		if (getDataCalculator() instanceof AggregationDataCalculator<?, ?>) {
			AggregationDataCalculator<ENTITY, ITEM> aggDC = (AggregationDataCalculator<ENTITY, ITEM>) getDataCalculator();
			settings.setAggregationMethod(aggDC.getAggregationMethod());
			settings.setAggregationFeature(aggDC.getAggregationFeature());
		}
		settings.setAxisLabels(getCustomAxisLabels());
		settings.setTitle(getTitle());
		return settings;
	}

	@Override
	public void setDataProviderSettings(DataProviderSettings<ENTITY, ITEM> settings) {
		setSelectedFeatures(settings.getFeatures());
		setAuxiliaryFeatures(settings.getAuxiliaryFeatures());
		Map<String, Object> properties = settings.getFilterProperties();
		if (filters == null) filters = createFilters();
		for (IFilter<ENTITY, ITEM> filter : filters) {
			Object o = properties.get(filter.getClass().getName());
			if (o != null) filter.setProperties(o);
		}
		setActiveGroupingStrategy(settings.getGroupingStrategy());
		if (getDataCalculator() instanceof AggregationDataCalculator<?, ?>) {
			AggregationDataCalculator<ENTITY, ITEM> aggDC = (AggregationDataCalculator<ENTITY, ITEM>) getDataCalculator();
			aggDC.setAggregationMethod(settings.getAggregationMethod());
			aggDC.setAggregationFeature(settings.getAggregationFeature());
		}
		setCustomAxisLabels(settings.getAxisLabels());
		setTitle(settings.getTitle());
	}

	@Override
	public IDataCalculator<ENTITY, ITEM> getDataCalculator() {
		if (dataCalculator == null) {
			dataCalculator = new AggregationDataCalculator<ENTITY, ITEM>(this);
		}
		return dataCalculator;
	}

	@Override
	public void setDataCalculator(IDataCalculator<ENTITY, ITEM> dataCalculator) {
		this.dataCalculator = dataCalculator;
	}

	private int getFeatureIndex(int dimension) {
		if (dimension < getSelectedFeatures().size()) {
			return getFeatureIndex(getSelectedFeatures().get(dimension));
		} else {
			return getFeatureIndex(getAuxiliaryFeatures().get(dimension - getSelectedFeatures().size()));
		}
	}

}