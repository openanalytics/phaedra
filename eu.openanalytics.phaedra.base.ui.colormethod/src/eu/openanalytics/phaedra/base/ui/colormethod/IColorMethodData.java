package eu.openanalytics.phaedra.base.ui.colormethod;

/**
 * Represents a data set whose elements can be visualized
 * by a color method.
 * 
 * E.g.
 * For a Min-Max color method, the data set should provide a minimum
 * and maximum value. All other values in the set will map to a color in
 * the min-max gradient. 
 */
public interface IColorMethodData {

	public double getMin();
	public double getMax();
	public double getMean();
	
	public double getValue(String name);
}
