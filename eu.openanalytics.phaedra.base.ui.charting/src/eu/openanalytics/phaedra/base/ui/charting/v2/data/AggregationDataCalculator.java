package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.RowSubset;

/**
 * <p>
 * A DataCalculator that performs aggregation on the data.
 * </p>
 * <ul>
 * <li>The aggregation feature is the feature on which aggregation will occur (combined with grouping).</li>
 * <li>The aggregation method is the statistic that is applied to each set of values.</li>
 * </ul>
 * <p>
 * Example: group on <i>Welltype</i>, aggregate on the feature <i>Timepoint</i>, using aggregation method <i>mean</i>.<br/>
 * Result: by grouping on <i>Welltype</i>, n wells end up in group X. For each <i>Timepoint</i>, there are n values in that group.
 * The <i>mean</i> of these n values is calculated and used in the plot.
 * </p>
 * <p>
 * If no aggregation method is specified, no aggregation will be performed.<br/>
 * If no aggregation feature is specified, each group will be aggregated into a single value.
 * </p>
 *
 */
// TODO: calculateDataBounds() is called before generateStarTable(). This often results in incorrect bounds.
public class AggregationDataCalculator<ENTITY, ITEM> extends DefaultDataCalculator<ENTITY, ITEM> {

	private String aggregationMethod;
	private String aggregationFeature;

	private double[][] currentBounds;
	private String[] currentFeatures;
	private BitsRowSubset[] currentGroups;
	private int[] aggregatedDataSizes;

	public final static String NONE = "<None>";
	public final static String[] AGGREGATION_METHODS = {NONE,"count","mad","max","mean","median","min","stdev","sum"};

	public AggregationDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider) {
		super(dataProvider);
		aggregationMethod = NONE;
		aggregationFeature = NONE;
	}

	public String getAggregationMethod() {
		return aggregationMethod;
	}

	public void setAggregationMethod(String aggregationMethod) {
		this.aggregationMethod = aggregationMethod;
	}

	public String getAggregationFeature() {
		return aggregationFeature;
	}

	public void setAggregationFeature(String aggregationFeature) {
		this.aggregationFeature = aggregationFeature;
	}

	@Override
	public StarTable generateStarTable() {

		if (!isAggregationMethodSet()) {
			return super.generateStarTable();
		}

		// Aggregation occurs on grouped data, so perform grouping now (before filtering!).
		BitsRowSubset[] groups = dataProvider.getActiveGroupingStrategy().groupData(dataProvider);
		if (groups == null) {
			groups = new BitsRowSubset[] { new BitsRowSubset("All", null)};
		}

		float[] aggFeatureValues = null;
		int aggFeatureIndex = dataProvider.getFeatureIndex(aggregationFeature);
		if (isAggregationFeatureSet() && aggFeatureIndex != -1) {
			// TODO: This might hang UI since it's not in a job.
			dataProvider.loadFeature(aggregationFeature, new NullProgressMonitor());
			aggFeatureValues = dataProvider.getColumnData(aggFeatureIndex, 1);
		}

		List<PrimitiveArrayColumn> columns = new ArrayList<>();
		int rowCount = 0;

		currentBounds = new double[dataProvider.getDimensionCount()][2];
		currentFeatures = dataProvider.getSelectedFeatures().toArray(new String[dataProvider.getDimensionCount()]);
		currentGroups = groups;
		aggregatedDataSizes = new int[currentGroups.length];

		for (int axis = 0; axis < dataProvider.getDimensionCount(); axis++) {
			String feature = currentFeatures[axis];
			if (feature == null) {
				feature = dataProvider.getSelectedFeature(axis);
				currentFeatures[axis] = feature;
			}

			double min = Double.MAX_VALUE;
			double max = 1-Double.MAX_VALUE;

			int featureIndex = dataProvider.getFeatureIndex(feature);
			float[] featureData = dataProvider.getColumnData(featureIndex, 1);

			List<float[]> aggregatedData = new ArrayList<>();

			for (int groupIndex=0; groupIndex<groups.length; groupIndex++) {
				BitSet group = groups[groupIndex].getBitSet();

				Map<Float, List<Double>> valuesToAggregate = new HashMap<>();
				if (aggFeatureValues == null) {
					List<Double> values = new ArrayList<Double>();
					for (int valueIndex=0; valueIndex<featureData.length; valueIndex++) {
						if (group != null && !group.get(valueIndex)) continue;
						values.add((double)featureData[valueIndex]);
					}
					valuesToAggregate.put(0.0f, values);
				} else {
					for (int valueIndex=0; valueIndex<aggFeatureValues.length; valueIndex++) {
						if (group != null && !group.get(valueIndex)) continue;
						float aggValue = aggFeatureValues[valueIndex];
						if (valuesToAggregate.get(aggValue) == null) valuesToAggregate.put(aggValue, new ArrayList<Double>());
						float value = featureData[valueIndex];
						valuesToAggregate.get(aggValue).add((double)value);
					}
				}

				// Reduce n Lists to float[n]
				float[] groupAggregatedData = new float[valuesToAggregate.size()];
				List<Float> aggregatedKeys = new ArrayList<>(valuesToAggregate.keySet());
				Collections.sort(aggregatedKeys);
				for (int i=0; i<aggregatedKeys.size(); i++) {
					Float key = aggregatedKeys.get(i);
					double[] values = CollectionUtils.toArray(valuesToAggregate.get(key));
					double value = Double.NaN;
					if (feature.equals(aggregationFeature) && values.length > 0) {
						// If this feature is the aggregation group feature itself, do not aggregate it.
						value = values[0];
					} else {
						value = calculateAggregation(values);
					}

					if (!Double.isNaN(value)) {
						if (value > max) max = value;
						if (value < min) min = value;
					}
					groupAggregatedData[i] = (float)value;
				}
				aggregatedData.add(groupAggregatedData);
				aggregatedDataSizes[groupIndex] = groupAggregatedData.length;
			}

			int totalSize = 0;
			for (float[] aggValues: aggregatedData) totalSize += aggValues.length;
			float[] allValues = new float[totalSize];
			int offset = 0;
			for (float[] aggValues: aggregatedData) {
				System.arraycopy(aggValues, 0, allValues, offset, aggValues.length);
				offset += aggValues.length;
			}

			currentBounds[axis] = new double[]{min, max};

			ColumnInfo columnInfo = new ColumnInfo(new DefaultValueInfo(feature, Float.class, feature));
			columns.add(PrimitiveArrayColumn.makePrimitiveColumn(columnInfo, allValues));
			rowCount = allValues.length;
		}

		BaseDataTable<ENTITY> columnStarTable = new BaseDataTable<ENTITY>(rowCount);
		for (PrimitiveArrayColumn column: columns) {
			columnStarTable.addColumn(column);
		}

		return columnStarTable;
	}

	@Override
	public RowSubset[] performGrouping() {

		if (!isAggregationMethodSet()) {
			return super.performGrouping();
		}

		// Use the grouping that was used when the table was built.
		RowSubset[] aggregatedGroups = new RowSubset[currentGroups.length];
		int offset = 0;
		for (int i=0; i<aggregatedGroups.length; i++) {
			BitsRowSubset group = currentGroups[i];

			int groupSize = aggregatedDataSizes[i];
			BitSet bitSet = new BitSet(dataProvider.getTotalRowCount());
			bitSet.set(offset, offset+groupSize, true);
			aggregatedGroups[i] = new BitsRowSubset(group.getName(), bitSet);
			offset += groupSize;
		}
		return aggregatedGroups;
	}

	@Override
	public double[][] calculateDataBounds() {

		if (!isAggregationMethodSet()){
			return super.calculateDataBounds();
		}

		// Generate the table (again) to update the bounds and Feature.
		generateStarTable();

		// Use the bounds that were calculated when the table was built.
		double[][] bounds = new double[dataProvider.getDimensionCount()][2];
		for (int dimension = 0; dimension<bounds.length; dimension++) {
			bounds[dimension] = currentBounds[dimension];
		}

		return bounds;
	}

	/*
	 * Non-public
	 * **********
	 */

	private double calculateAggregation(double[] values) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double v: values) {
			if (Double.isNaN(v)) continue;
			stats.addValue(v);
		}
		double retVal = Double.NaN;
		switch (aggregationMethod) {
		case "count":
			retVal = stats.getValues().length;
			break;
		case "mad":
			double mean = stats.getMean();
			DescriptiveStatistics stats2 = new DescriptiveStatistics();
			for (double v: stats.getValues()) stats2.addValue(Math.abs(v - mean));
			retVal = stats2.getMean();
			break;
		case "max":
			retVal = stats.getMax();
			break;
		case "mean":
			retVal = stats.getMean();
			break;
		case "median":
			retVal = stats.getPercentile(50.0d);
			break;
		case "min":
			retVal = stats.getMin();
			break;
		case "stdev":
			retVal = stats.getStandardDeviation();
			break;
		case "sum":
			retVal = stats.getSum();
			break;
		}
		return retVal;
	}
	
	private boolean isAggregationMethodSet() {
		return (aggregationMethod != null && !aggregationMethod.equals(NONE));
	}

	private boolean isAggregationFeatureSet() {
		return (aggregationFeature != null && !aggregationFeature.equals(NONE));
	}
}
