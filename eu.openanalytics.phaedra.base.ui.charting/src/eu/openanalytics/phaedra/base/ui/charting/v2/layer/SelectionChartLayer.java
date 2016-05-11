package eu.openanalytics.phaedra.base.ui.charting.v2.layer;

import java.awt.Shape;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.selection.SelectionChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;

public class SelectionChartLayer<ENTITY, ITEM> extends AbstractChartLayer<ENTITY, ITEM> {

	public SelectionChartLayer() {
		super(new SelectionChart<ENTITY, ITEM>(), null);
	}

	@Override
	public void initializeChartLayer(IDataProvider<ENTITY, ITEM> dataProvider) {
		// Do nothing.
	}

	@Override
	public void dataChanged() {
		// Do nothing.
	}

	@Override
	public void settingsChanged() {
		// Do nothing.
	}

	public Shape getSelection() {
		return ((SelectionChart<ENTITY, ITEM>)getChart()).getArea();
	}

	public boolean isSingleSelection() {
		return ((SelectionChart<ENTITY, ITEM>) getChart()).isSingleSelection();
	}

}