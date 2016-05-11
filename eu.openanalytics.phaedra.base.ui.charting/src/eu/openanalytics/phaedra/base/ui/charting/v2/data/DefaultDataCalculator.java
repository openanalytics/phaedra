package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.BitSet;

import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;

public class DefaultDataCalculator<ENTITY, ITEM> implements IDataCalculator<ENTITY, ITEM> {

	IDataProvider<ENTITY, ITEM> dataProvider;

	public DefaultDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider) {
		super();
		this.dataProvider = dataProvider;
	}

	@Override
	public StarTable generateStarTable() {
		BaseDataTable<ENTITY> columnStarTable = new BaseDataTable<ENTITY>(dataProvider.getTotalRowCount());

		int axis = 0;
		for (String feature : dataProvider.getSelectedFeatures()) {
			int index = dataProvider.getFeatureIndex(feature);
			float[] rowArray = dataProvider.getColumnData(index, axis++);
			PrimitiveArrayColumn column = PrimitiveArrayColumn.makePrimitiveColumn(dataProvider.getColumnInfo(index),
					rowArray);
			columnStarTable.addColumn(column);
		}

		// handle the auxiliary axes
		if (dataProvider.getAuxiliaryFeatures() != null) {
			for (String auxFeature : dataProvider.getAuxiliaryFeatures()) {
				int auxiliaryIndex = dataProvider.getFeatureIndex(auxFeature);
				float[] rowArray = dataProvider.getColumnData(auxiliaryIndex, axis++);
				PrimitiveArrayColumn column = PrimitiveArrayColumn.makePrimitiveColumn(
						dataProvider.getColumnInfo(auxiliaryIndex), rowArray);
				columnStarTable.addColumn(column);
			}
		}

		return columnStarTable;
	}

	@Override
	public RowSubset[] performGrouping() {
		return dataProvider.getActiveGroupingStrategy().groupData(dataProvider);
	}

	@Override
	public double[][] calculateDataBounds() {
		double[][] bounds = new double[dataProvider.getDimensionCount()][2];

		for (int dimension = 0; dimension < bounds.length; dimension++) {
			bounds[dimension] = dataProvider.calculateDataBoundsForDimension(dimension);
		}
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