package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot.Style;

public interface IDataProvider<ENTITY, ITEM> {

	ColumnInfo getColumnInfo(int column);

	float[] getColumnData(int col, int axis);

	List<String> getFeatures();

	String getSelectedFeature(int axis);

	void setSelectedFeature(String feature, int axis, IProgressMonitor monitor);

	List<String> getSelectedFeatures();

	void setSelectedFeatures(List<String> selectedFeatures);

	void setSelectedFeatures(List<String> selectedFeatures, IProgressMonitor monitor);

	void setAuxilaryFeature(String feature, int axis);

	void setAuxilaryFeature(String feature, int axis, IProgressMonitor monitor);

	void setAuxiliaryFeatures(List<String> auxiliaryFeatures);

	List<String> getAuxiliaryFeatures();

	double[][] getDataBounds();

	void setDataBounds(double[][] bounds);

	void loadData(List<ITEM> data, int dimensions);

	void loadData(List<ITEM> data, int dimensions, IProgressMonitor monitor);

	void loadFeature(String feature, IProgressMonitor monitor);

	void initialize();

	ENTITY getKey(long row);

	int[] getKeyRange(ENTITY key);

	Object getRowObject(long row);

	Map<ENTITY, Integer> getDataSizes();

	ISelection createSelection(BitSet selectionBitSet);

	BitSet createSelection(List<?> entities);

	String[] getGates();

	int getTotalRowCount();

	int getDimensionCount();

	BitSet getCurrentFilter();

	StarTable generateStarTable();

	IGroupingStrategy<ENTITY, ITEM> getActiveGroupingStrategy();

	RowSubset[] performGrouping();

	void performFiltering();

	void setActiveGroupingStrategy(IGroupingStrategy<ENTITY, ITEM> groupingStrategy);

	Style[] getStyles(ChartSettings settings);

	String[] getAxisLabels();

	String[] getCustomAxisLabels();

	void setCustomAxisLabels(String[] customAxisLabels);

	String[] getAxisValueLabels(String feature);

	List<IFilter<ENTITY, ITEM>> getFilters();

	void setFilters(List<IFilter<ENTITY, ITEM>> filters);

	int getFeatureIndex(String feature);

	double[][] calculateDatabounds();

	void setAvailableNumberOfPoints(int potentialPointCount);

	void setSelectedNumberOfPoints(int includedPointCount);

	void setVisibleNumberOfPoints(int visiblePointCount);

	int getAvailableNumberOfPoints();

	int getSelectedNumberOfPoints();

	int getVisibleNumberOfPoints();

	ValueObservable getDataChangedObservable();

	ValueObservable getTitleChangedObservable();

	ValueObservable getLabelChangedObservable();

	List<ENTITY> getCurrentEntities();

	List<ITEM> getCurrentItems();

	double[] calculateDataBoundsForDimension(int dimension);

	DataProviderSettings<ENTITY, ITEM> getDataProviderSettings();

	void setDataProviderSettings(DataProviderSettings<ENTITY, ITEM> settings);

	void setTotalRowCount(int i);

	void setDataCalculator(IDataCalculator<ENTITY, ITEM> dataCalculator);

	IDataCalculator<ENTITY, ITEM> getDataCalculator();

	Map<String, List<String>> getFeaturesPerGroup();

	String getTitle();

	void setTitle(String title);

}