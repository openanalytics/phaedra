package eu.openanalytics.phaedra.base.ui.charting.data;

import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.axis.NumberAxis;

public abstract class SimpleDataProvider<E> implements IDataProvider<E> {

	private E[] items;
	
	public void setItems(E[] items) {
		this.items = items;
	}
	
	@Override
	public int getSeriesCount() {
		return 1;
	}
	
	@Override
	public String getSeriesName(int index) {
		return "Series " + index;
	}

	@Override
	public List<E> buildSeries(int index) {
		List<E> data = new ArrayList<E>();
		for (int i=0; i<items.length; i++) {
			data.add(items[i]);
		}
		return data;
	}
	
	@Override
	public NumberAxis createAxis(int dimension, String parameter) {
		NumberAxis xAxis = new NumberAxis(parameter);
		xAxis.setAutoRangeIncludesZero(false);
		return xAxis;
	}	
}
