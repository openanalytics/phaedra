package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import uk.ac.starlink.table.ColumnStarTable;

public class BaseDataTable<ENTITY> extends ColumnStarTable {

	private int rowCount;

	public BaseDataTable(int rowCount) {
		this.rowCount = rowCount;
	}

	@Override
	public long getRowCount() {
		return rowCount;
	}
}