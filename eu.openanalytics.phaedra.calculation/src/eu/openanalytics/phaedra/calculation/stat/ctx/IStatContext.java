package eu.openanalytics.phaedra.calculation.stat.ctx;

import eu.openanalytics.phaedra.calculation.stat.filter.IFilter;


public interface IStatContext {

	public int getDataSets();
	
	public double[] getData(int index);
	
	public void applyFilter(IFilter filter);
}
