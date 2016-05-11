package eu.openanalytics.phaedra.base.ui.charting.data;

import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.NumberAxis;

public interface IDataProvider<E> {

	public int getSeriesCount();

	public String getSeriesName(int seriesIndex);

	public List<E> buildSeries(int seriesIndex);

	public String[] getParameters();

	public Map<String, List<String>> getGroupedFeatures();

	public double[] getValue(E item, String[] parameters, int row);

	public String getLabel(E item);

	public NumberAxis createAxis(int dimension, String parameter);

	double[] getGlobalMinMax(String[] parameters);

}
