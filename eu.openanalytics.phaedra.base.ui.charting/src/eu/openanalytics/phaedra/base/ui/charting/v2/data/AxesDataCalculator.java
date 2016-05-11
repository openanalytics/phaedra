package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;

public class AxesDataCalculator<ENTITY, ITEM> extends DefaultDataCalculator<ENTITY, ITEM> {

	public AxesDataCalculator(IDataProvider<ENTITY, ITEM> dataProvider) {
		super(dataProvider);
	}

	@Override
	public StarTable generateStarTable() {
		return new BaseDataTable<ENTITY>(0);
	}

	@Override
	public RowSubset[] performGrouping() {
		return null;
	}

	@Override
	public double[][] calculateDataBounds() {
		// Axes1D is still a 2D chart internally.
		// Because of that we need 2 bounds instead of 1.
		return new double[][] { new double[2], new double[2] };
	}

}