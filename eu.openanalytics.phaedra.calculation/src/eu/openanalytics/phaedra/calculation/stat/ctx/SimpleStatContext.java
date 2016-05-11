package eu.openanalytics.phaedra.calculation.stat.ctx;

import eu.openanalytics.phaedra.calculation.stat.filter.IFilter;

public class SimpleStatContext implements IStatContext {

	private double[][] data;
	
	public SimpleStatContext(double[] data) {
		this.data = new double[][]{data};
	}
	
	public SimpleStatContext(double[][] data) {
		this.data = data;
	}
	
	@Override
	public int getDataSets() {
		return data.length;
	}

	@Override
	public double[] getData(int index) {
		return data[index];
	}

	@Override
	public void applyFilter(IFilter filter) {
		for (int i=0; i<data.length; i++) data[i] = filter.apply(data[i]);
	}
}
