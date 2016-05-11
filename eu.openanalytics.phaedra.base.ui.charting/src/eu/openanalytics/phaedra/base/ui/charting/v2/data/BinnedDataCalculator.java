package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;

import java.util.Set;
import java.util.TreeMap;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.RowSubset;

public class BinnedDataCalculator<ENTITY, ITEM> implements IDataCalculator<ENTITY, ITEM> {

	private IDataProvider<ENTITY, ITEM> dataProvider;
	private double[][] bounds;
	private int numberOfBins;
	private TreeMap<Double, Bin> bins = new TreeMap<Double, Bin>();
	private int numberOfGroups = 1;
	private boolean logaritmic;
	private boolean cumulative;
	private boolean normalized;
	private ChartSettings settings;
	private int totalRowCount = 0;
	private float[] originalData;
	private float[][] binnedColumnData;
	private double binWidth;
	private BitsRowSubset[] groups;

	public BinnedDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider, ChartSettings settings) {
		super();
		this.dataProvider = dataProvider;
		this.settings = settings;
		this.numberOfBins = settings.getNumberOfBins();
		this.logaritmic = settings.isLogaritmic();
		this.cumulative = settings.isCumulative();
		this.normalized = settings.isNormalized();
	}

	@Override
	public StarTable generateStarTable() {
		int axis = 0;
		bins.clear();
		String feature = dataProvider.getSelectedFeatures().get(axis);
		float[][] rowArray = getBinnedColumnData(dataProvider.getFeatureIndex(feature), axis);

		BaseDataTable<ENTITY> rColumnStarTable = new BaseDataTable<ENTITY>(rowArray[0].length);
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				feature, Float.class, feature)), rowArray[0]));
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				"Frequency", Float.class, "Frequency")), rowArray[1]));

		return rColumnStarTable;
	}

	private float[][] getBinnedColumnData(int featureIndex, int axis) {
		originalData = dataProvider.getColumnData(featureIndex, axis);

		// Get min and max
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		binWidth = 1;
		for (int j = 0; j < originalData.length; j++) {
			if (!Float.isNaN(originalData[j]) && !Float.isInfinite(originalData[j])) {
				xMin = Math.min(xMin, originalData[j]);
				xMax = Math.max(xMax, originalData[j]);
			}
		}

		if (xMin < xMax) {
			binWidth = (xMax - xMin) / Math.max(1, numberOfBins - 2);
		}

		// add extra padding to xMin and xMax to allow for bar to be drawn on plot
		xMin -= binWidth / 2;
		xMax += binWidth / 2;

		settings.setBinWidth(binWidth);

		// group data
		groups = dataProvider.getActiveGroupingStrategy().groupData(dataProvider);
		numberOfGroups = groups != null ? groups.length : 1;

		// initialize all bins (also those without values)
		double startKey = Math.floor(xMin / binWidth);
		for (int i = 0; i < numberOfBins; i++) {
			Bin bin = new Bin(startKey + i, binWidth, numberOfGroups);
			bins.put(startKey + i, bin);
		}
		for (int group = 0; group < numberOfGroups; group++) {
			BitsRowSubset subset = groups != null ? groups[group] : null;
			for (int i = 0; i < originalData.length; i++) {
				float value = originalData[i];
				if (!Float.isNaN(value) && !Float.isInfinite(value)) {
					if (subset == null || subset.getBitSet().get(i)) {
						Bin bin = getBin(value);
						bin.binValue(value, group);
					}
				}
			}
		}

		binnedColumnData = new float[2][bins.size() * numberOfGroups];

		double yMin = 0;
		double yMax = 0;
		int rowCount = 0;
		float[] totalSum = new float[numberOfGroups];
		for (Bin bin : bins.values()) {
			for (int group = 0; group < numberOfGroups; group++) {
				if (cumulative) {
					totalSum[group] += bin.getSum(group);
				} else {
					totalSum[group] += bin.getSum(group) * binWidth;
				}
			}
		}

		// calculate relative frequency...
		float[] totalValue = new float[numberOfGroups];
		for (Bin bin : bins.values()) {
			for (int group = 0; group < numberOfGroups; group++) {
				float x = (float) (bin.getKey() * binWidth);
				float y = (float) bin.getSum(group);
				if (normalized && totalSum[group] != 0) {
					y = y / totalSum[group];
				}
				if (logaritmic && y != 0) {
					y = (float) Math.log(y);
				}
				if (cumulative) {
					y += totalValue[group];
					totalValue[group] = y;
				}
				binnedColumnData[0][rowCount] = x;
				binnedColumnData[1][rowCount] = y;
				yMax = Math.max(yMax, y);

				rowCount++;
			}
		}

		if (bins.isEmpty()) {
			bounds = new double[][] { { 0, 0 }, { 0, 0 } };
		} else {
			// Get the corrected bounds, taking plate limits into account.
			double[] correctedBounds = dataProvider.calculateDataBoundsForDimension(axis);
			correctedBounds[0] -= binWidth / 2;
			correctedBounds[1] += binWidth / 2;
			bounds = new double[][] { { correctedBounds[0], correctedBounds[1] }, { yMin, yMax } };
		}
		totalRowCount = bins.size() * numberOfGroups;
		return binnedColumnData;
	}

	private Bin getBin(float value) {
		Double key = Math.floor(value / binWidth);
		Bin bin = bins.get(key);

		if (bin == null) {
			Entry<Double, Bin> floorEntry = bins.floorEntry(key);
			if (floorEntry != null)	bin = floorEntry.getValue();
		}

		if (bin != null) {
			if (value > bin.getHighBound()) {
				Bin newBin = bins.get(key + 1);
				if (newBin != null) {
					bin = newBin;
				}
			} else if (value < bin.getLowBound()) {
				Bin newBin = bins.get(key - 1);
				if (newBin != null) {
					bin = newBin;
				}
			}
		}

		return bin;
	}

	@Override
	public RowSubset[] performGrouping() {
		BitsRowSubset[] subsets = new BitsRowSubset[numberOfGroups];
		for (int groupIndex = 0; groupIndex < numberOfGroups; groupIndex++) {
			BitSet bitSet = new BitSet(totalRowCount);
			subsets[groupIndex] = new BitsRowSubset(String.valueOf(groupIndex), bitSet);
		}
		for (int i = 0; i < totalRowCount; i++) {
			int group = i % numberOfGroups;
			for (int groupIndex = 0; groupIndex < numberOfGroups; groupIndex++) {
				subsets[groupIndex].getBitSet().set(i, group == groupIndex);
			}
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
				Bin bin = getBin(originalData[i]);

				if (bin != null) {
					int binIndex = 0;
					for (Double binKey : bins.keySet()) {
						if (binKey == bin.getKey()) {
							break;
						}
						binIndex++;
					}
					if (groups != null) {
						for (int group = 0; group < groups.length; group++) {
							BitsRowSubset bitsetGroup = groups[group];
							if (bitsetGroup.getBitSet().get(i)) {
								int dataIndex = binIndex > 0 ? binIndex * numberOfGroups + group : binIndex;
								selectedData.set(dataIndex, true);
								break;
							}
						}
					} else {
						selectedData.set(binIndex, true);
					}
				}
			}
		}
		return selectedData;
	}

	@Override
	public BitSet deCalculateSelection(BitSet binnedPoints) {
		BitSet selectedData = new BitSet(originalData.length);

		List<Bin> selectedBins = new ArrayList<Bin>();
		List<Bin> binArray = new ArrayList<>(bins.values());
		Set<Integer> selectedGroup = new HashSet<>();

		for (int i = binnedPoints.nextSetBit(0); i >= 0; i = binnedPoints.nextSetBit(i+1)) {
			selectedGroup.add(i % numberOfGroups);
			int binIndex = i / numberOfGroups;
			selectedBins.add(binArray.get(binIndex));
		}

		selectedBins.parallelStream().forEach(bin -> {
			for (int i = 0; i < originalData.length; i++) {
				if (originalData[i] >= bin.getLowBound() && originalData[i] <= bin.getHighBound()) {
					if (groups == null) {
						selectedData.set(i, true);
					} else {
						for (int group : selectedGroup) {
							if (groups[group].getBitSet().get(i)) {
								selectedData.set(i, true);
							}
						}
					}
				}
			}
		});

		return selectedData;
	}

	private class Bin {
		final double[] sums;
		final double key;
		final double binWidth;
		private double lowBound;
		private double highBound;

		private Bin(double key, double width, int groupCount) {
			super();
			this.sums = new double[groupCount];
			this.key = key;
			this.binWidth = width;
			this.lowBound = (key - 0.5) * binWidth;
			this.highBound = (key + 0.5) * binWidth;
		}

		public double getLowBound() {
			return lowBound;
		}

		public double getHighBound() {
			return highBound;
		}

		public void binValue(double value, int group) {
			sums[group] += 1;
		}

		public double getSum(int group) {
			return sums[group];
		}

		public double getKey() {
			return key;
		}
	}
}