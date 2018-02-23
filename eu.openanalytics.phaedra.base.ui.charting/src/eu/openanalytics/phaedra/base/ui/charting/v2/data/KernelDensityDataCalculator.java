package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.data.RVector;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.statet.rj.services.FunctionCall;

import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.RowSubset;

public class KernelDensityDataCalculator<ENTITY, ITEM> implements IDataCalculator<ENTITY, ITEM> {
	private static final int R_NUMBER_OF_ITEMS = 512;
	private IDataProvider<ENTITY, ITEM> dataProvider;
	private double[][] bounds;

	public KernelDensityDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider) {
		super();
		this.dataProvider = dataProvider;
	}

	@Override
	public StarTable generateStarTable() {
		int axis = 0;
		String feature = dataProvider.getSelectedFeatures().get(axis);
		float[][] rowArray = getRColumnData(dataProvider.getFeatureIndex(feature), axis);

		BaseDataTable<ENTITY> rColumnStarTable = new BaseDataTable<ENTITY>(rowArray[0].length);
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				feature, Float.class, feature)), rowArray[0]));
		rColumnStarTable.addColumn(PrimitiveArrayColumn.makePrimitiveColumn(new ColumnInfo(new DefaultValueInfo(
				"Count", Float.class, "Count")), rowArray[1]));

		return rColumnStarTable;
	}

	@SuppressWarnings("rawtypes")
	private float[][] getRColumnData(int col, int axis) {

		float[] originalColumnData = dataProvider.getColumnData(col, axis);
		float[][] rColumnData = null;

		// get values from R routine
		RServi rSession = null;
		try {
			rSession = RService.getInstance().createSession();
			rSession.evalVoid("library(PHAEDRA)", null);

			// grouping
			HashMap<String, double[]> groupedData = createGroupedData(originalColumnData);
			StringBuilder sb = new StringBuilder();
			sb.append("wellList <- list(");
			for (String groupName : groupedData.keySet()) {
				double[] values = groupedData.get(groupName);
				if (values.length != 0) {
					rSession.assignData(groupName, RUtils.makeNumericRVector(values), null);
					sb.append(groupName + ",");
				}
			}
			sb.deleteCharAt(sb.length()-1);
			rSession.evalVoid(sb.toString(), null);

			String[] groupNames = new String[groupedData.size()];
			rColumnData = new float[2][R_NUMBER_OF_ITEMS * groupNames.length];

			String[] colors = new String[groupedData.size()];
			Iterator<String> it = groupedData.keySet().iterator();
			for (int i = 0; i < groupNames.length; i++) {
				groupNames[i] = "'" + it.next() + "'";
				colors[i] = "'#0000FF'";
			}

			rSession.evalVoid("wellLegend <- c(" + StringUtils.createSeparatedString(colors, ",") + ")", null);
			rSession.evalVoid("names(wellLegend) <- c(" + StringUtils.createSeparatedString(groupNames, ",") + ")", null);

			FunctionCall rFunctionCall = rSession.createFunctionCall("multipleWellDensityPlot");
			rFunctionCall.add("zList", "wellList");
			rFunctionCall.add("legend", "wellLegend");
			rFunctionCall.addChar("main", dataProvider.getSelectedFeature(axis));
			// if (minimal) rFunctionCall.add("minimal", "TRUE");

			rSession.evalVoid("pdf()", null);
			RList returnValues = (RList) rFunctionCall.evalData(null);

			// for all groups
			bounds = new double[][] { { Double.MAX_VALUE, Double.MIN_VALUE }, { Double.MAX_VALUE, Double.MIN_VALUE } };
			for (int groupIndex = 0; groupIndex < groupNames.length; groupIndex++) {
				String group = groupNames[groupIndex];
				RList items = (RList) returnValues.get(escape(group));
				if (items != null) {
					RVector x = (RVector) items.get("x");
					RVector y = (RVector) items.get("y");

					// assign the data
					float previousX = Float.MIN_VALUE;
					for (int i = 0; i < x.getLength(); i++) {
						int dataPosition = groupIndex * R_NUMBER_OF_ITEMS + i;
						float currentX = (float) x.getData().getNum(i);
						bounds[0][0] = Math.min(bounds[0][0], currentX);
						bounds[0][1] = Math.max(bounds[0][1], currentX);
						rColumnData[0][dataPosition] = currentX;
						// TODO filter out negative values? Why are they there anyway?
						float yValue = Math.max(0, (float) y.getData().getNum(i) * (currentX - previousX));
						rColumnData[1][dataPosition] = yValue;
						bounds[1][0] = Math.min(bounds[1][0], yValue);
						bounds[1][1] = Math.max(bounds[1][1], yValue);
						previousX = currentX;
					}
				}
			}
			dataProvider.setTotalRowCount(groupNames.length * R_NUMBER_OF_ITEMS);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create density plot", e);
		} finally {
			if (rSession != null) {
				try {
					rSession.close();
				} catch (CoreException e) {
				}
			}
		}

		return rColumnData;
	}

	private HashMap<String, double[]> createGroupedData(float[] filteredColumns) {
		HashMap<String, double[]> dataGroups = new HashMap<String, double[]>();

		// group data
		BitsRowSubset[] groups = dataProvider.getActiveGroupingStrategy().groupData(dataProvider);

		if (groups != null) {
			for (BitsRowSubset subset : groups) {
				double[] values = new double[subset.getBitSet().cardinality()];
				int index = 0;
				for (int i = 0; i < filteredColumns.length; i++) {
					if (subset.getBitSet().get(i)) {
						values[index++] = filteredColumns[i];
					}
				}
				dataGroups.put(escape(subset.getName()), values);
			}
		} else {
			double[] values = new double[filteredColumns.length];
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
		int numberOfGroups = dataProvider.getTotalRowCount() / R_NUMBER_OF_ITEMS;
		BitsRowSubset[] subsets = new BitsRowSubset[numberOfGroups];
		for (int groupIndex = 0; groupIndex < numberOfGroups; groupIndex++) {
			BitSet bitSet = new BitSet(dataProvider.getTotalRowCount());
			bitSet.set(groupIndex * R_NUMBER_OF_ITEMS, (groupIndex + 1) * R_NUMBER_OF_ITEMS - 1, true);
			subsets[groupIndex] = new BitsRowSubset("" + groupIndex, bitSet);
		}

		return subsets;
	}

	@Override
	public double[][] calculateDataBounds() {
		return bounds;
	}

	@Override
	public BitSet calculateSelection(BitSet containedPoints) {
		return containedPoints;
	}

	@Override
	public BitSet deCalculateSelection(BitSet containedPoints) {
		return containedPoints;
	}
}