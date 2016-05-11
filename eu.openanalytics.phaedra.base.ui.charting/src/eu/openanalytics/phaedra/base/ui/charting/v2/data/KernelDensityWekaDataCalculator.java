package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.RowSubset;
import weka.estimators.KernelEstimator;

public class KernelDensityWekaDataCalculator<ENTITY, ITEM> implements IDataCalculator<ENTITY, ITEM> {

	public static final String PRECISION_VALUE = "PRECISION_VALUE";
	public static final String DEFAULT_PRECISION_VALUE = 1000 + "";

	private static final int NUMBER_OF_PAINTINGPOINTS_PER_BIN = 10;
	private static final double WEIGHT = 1.0;
	private IDataProvider<ENTITY, ITEM> dataProvider;
	private ChartSettings chartSettings;
	private double[][] bounds;
	private int numberOfBins;
	private boolean cumulative;
	private int totalRowCount = 0;
	private float[] originalData;
	private float[][] binnedColumnData;
	private BitsRowSubset[] groups;
	private int numberOfGroups;
	private double xMin = Double.MAX_VALUE;
	private double xMax = Double.MIN_VALUE;
	private float binWidth;

	public KernelDensityWekaDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider, ChartSettings chartSettings) {
		super();
		this.dataProvider = dataProvider;
		this.chartSettings = chartSettings;
		this.cumulative = chartSettings.isCumulative();
	}

	@Override
	public StarTable generateStarTable() {
		numberOfBins = chartSettings.getNumberOfBins() * NUMBER_OF_PAINTINGPOINTS_PER_BIN;

		int axis = 0;
		String feature = dataProvider.getSelectedFeatures().get(axis);
		float[][] rowArray = getWekaColumnData(dataProvider.getFeatureIndex(feature), axis);

		BaseDataTable<ENTITY> rColumnStarTable = new BaseDataTable<ENTITY>(rowArray[0].length);
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				feature, Float.class, feature)), rowArray[0]));
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				"Frequency", Float.class, "Frequency")), rowArray[1]));

		return rColumnStarTable;
	}

	private float[][] getWekaColumnData(int col, int axis) {
		originalData = dataProvider.getColumnData(col, axis);
		binnedColumnData = null;

		// Get values from WEKA routine
		bounds = new double[][] { { Double.MAX_VALUE, Double.MIN_VALUE }, { Double.MAX_VALUE, Double.MIN_VALUE } };
		xMin = Double.MAX_VALUE;
		xMax = Double.MIN_VALUE;
		for (double d : originalData) {
			if (!Double.isNaN(d) && !Double.isInfinite(d)) {
				xMin = Math.min(d, xMin);
				xMax = Math.max(d, xMax);
			}
		}
		/*
		 * Originally, the value 0.001f was used. Now it's calculated from the minimum and maximum.
		 *
		 * The precision to which numeric values are given. For example, if the precision is stated to be 0.1,
		 * the values in the interval (0.25,0.35] are all treated as 0.3.
		 */
		chartSettings.getMiscSettings().putIfAbsent(PRECISION_VALUE, DEFAULT_PRECISION_VALUE);
		int precisionValue = chartSettings.getIntMiscSetting(PRECISION_VALUE);
		float precision = (float) ((xMax - xMin) / precisionValue);

		// Grouping
		Map<String, float[]> groupedData = createGroupedData(originalData);
		Map<String, KernelEstimator> estimatorMap = new HashMap<String, KernelEstimator>();
		for (String group : groupedData.keySet()) {
			KernelEstimator estimator = new KernelEstimator(precision);
			float[] groupData = groupedData.get(group);
			for (float value : groupData) {
				if (!Double.isNaN(value) && !Double.isInfinite(value)) {
					estimator.addValue(value, WEIGHT);
				}
			}
			estimatorMap.put(group, estimator);
		}
		bounds[0][0] = xMin;
		bounds[0][1] = xMax;
		binWidth = (float) (xMax - xMin) / Math.max(1, numberOfBins - 1);

		// Get probabilities
		binnedColumnData = new float[2][numberOfBins * groupedData.keySet().size()];
		int groupIndex = 0;

		for (String group : groupedData.keySet()) {
			float totalValue = 0;
			KernelEstimator estimator = estimatorMap.get(group);

			// Get the biggest Y Value, only used for cumulative.
			float maxYValue = 0f;

			for (int i = 0; i < numberOfBins; i++) {
				int dataPosition = groupIndex * numberOfBins + i;
				float xValue = (float) (xMin + i * binWidth);
				float yValue = Math.max(0, (float) estimator.getProbability(xValue) / precision);
				if (cumulative) {
					totalValue += yValue;
					yValue = totalValue;
					maxYValue = yValue;
				}
				binnedColumnData[0][dataPosition] = xValue;
				binnedColumnData[1][dataPosition] = yValue;
				bounds[1][0] = Math.min(bounds[1][0], yValue);
				bounds[1][1] = Math.max(bounds[1][1], yValue);
			}
			if (cumulative) {
				bounds[1][1] = 0;
				for (int i = 0; i < numberOfBins; i++) {
					int dataPosition = groupIndex * numberOfBins + i;
					binnedColumnData[1][dataPosition] /= maxYValue;
					bounds[1][1] = Math.max(bounds[1][1], binnedColumnData[1][dataPosition]);
				}
			}
			groupIndex++;
		}

		// Get the corrected bounds, taking plate limits into account.
		double[] correctedBounds = dataProvider.calculateDataBoundsForDimension(axis);
		correctedBounds[0] -= binWidth / 2;
		correctedBounds[1] += binWidth / 2;
		bounds = new double[][] { { correctedBounds[0], correctedBounds[1] }, { bounds[1][0], bounds[1][1] } };

		totalRowCount = groupedData.keySet().size() * numberOfBins;
		return binnedColumnData;
	}

	private HashMap<String, float[]> createGroupedData(float[] filteredColumns) {
		LinkedHashMap<String, float[]> dataGroups = new LinkedHashMap<String, float[]>();

		// Group data
		groups = dataProvider.getActiveGroupingStrategy().groupData(dataProvider);
		numberOfGroups = groups != null ? groups.length : 1;

		if (groups != null) {
			for (BitsRowSubset subset : groups) {
				float[] values = new float[subset.getBitSet().cardinality()];
				int index = 0;
				for (int i = 0; i < filteredColumns.length; i++) {
					if (subset.getBitSet().get(i)) {
						values[index++] = filteredColumns[i];
					}
				}
				dataGroups.put(escape(subset.getName()), values);
			}
		} else {
			float[] values = new float[filteredColumns.length];
			for (int i = 0; i < filteredColumns.length; i++) {
				values[i] = filteredColumns[i];
			}
			dataGroups.put(escape(dataProvider.getActiveGroupingStrategy().getName()), values);
		}
		return dataGroups;
	}

	private String escape(String name) {
		if (name != null) {
			return name.replace(" ", "_").replace("+", "plus").replace("'", "");
		}
		return null;
	}

	@Override
	public RowSubset[] performGrouping() {
		int numberOfGroups = totalRowCount / numberOfBins;
		BitsRowSubset[] subsets = new BitsRowSubset[numberOfGroups];
		for (int groupIndex = 0; groupIndex < numberOfGroups; groupIndex++) {
			BitSet bitSet = new BitSet(totalRowCount);
			bitSet.set(groupIndex * numberOfBins, (groupIndex + 1) * numberOfBins - 1, true);
			subsets[groupIndex] = new BitsRowSubset("" + groupIndex, bitSet);
		}

		return subsets;
	}

	@Override
	public double[][] calculateDataBounds() {
		generateStarTable();
		return bounds;
	}

	@Override
	public BitSet calculateSelection(BitSet containedPoints) {
		if (binnedColumnData == null) return new BitSet();

		BitSet selectedData = new BitSet(binnedColumnData[0].length);
		for (int i = 0; i < originalData.length; i++) {
			if (containedPoints.get(i)) {
				// calculate bin
				int binIndex = 0;
				for (int j = 0; j < numberOfBins; j++) {
					if (originalData[i] > (xMin + j * binWidth) && originalData[i] < (xMin + (j + 1) * binWidth)) {
						binIndex = j;
					}
				}

				if (groups != null) {
					for (int group = 0; group < groups.length; group++) {
						BitsRowSubset bitsetGroup = groups[group];
						if (bitsetGroup.getBitSet().get(i)) {
							int dataIndex = group * numberOfBins + binIndex;
							selectedData.set(dataIndex, true);
							break;
						}
					}
				} else {
					selectedData.set(binIndex, true);
				}
			}
		}
		return selectedData;
	}

	@Override
	public BitSet deCalculateSelection(BitSet binnedPoints) {
		BitSet selectedData = new BitSet(originalData.length);
		if (binnedPoints != null) {
			HashMap<Integer, boolean[]> binGroups = new HashMap<Integer, boolean[]>();
			for (int i = 0; i < binnedPoints.length(); i++) {
				if (binnedPoints.get(i)) {
					int bin = i % numberOfBins;
					int group = i/numberOfBins;

					boolean[] groupsOfBin = binGroups.get(bin);
					if(groupsOfBin == null){
						groupsOfBin = new boolean[numberOfGroups];
						binGroups.put(bin, groupsOfBin);
					}
					groupsOfBin[group] = true;
				}
			}

			for(int bin : binGroups.keySet()){
				boolean[] groupsOfBin = binGroups.get(bin);

				float binLow = (float) (xMin + bin * binWidth);
				float binHigh = (float) (xMin + (bin + 1) * binWidth);

				for (int i = 0; i < originalData.length; i++) {
					for(int group = 0; group < numberOfGroups; group++) {
						if (groupsOfBin[group]) {
							if (groups != null) {
								BitsRowSubset groupSubset = groups[group];

								if (groupSubset.getBitSet().get(i)) {
									if (originalData[i] >= binLow && originalData[i] <= binHigh) {
										selectedData.set(i, true);
									}
								}
							} else {
								if (originalData[i] >= binLow && originalData[i] <= binHigh) {
									selectedData.set(i, true);
								}
							}
						}
					}
				}
			}
		}
		return selectedData;
	}
}