package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.BitSet;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;

public interface IDataCalculator<ENTITY, ITEM> {

	StarTable generateStarTable();

	RowSubset[] performGrouping();

	double[][] calculateDataBounds();

	BitSet calculateSelection(BitSet containedPoints);

	BitSet deCalculateSelection(BitSet containedPoints);

}