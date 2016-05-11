package eu.openanalytics.phaedra.calculation.stat;

import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public interface IStatCalculator {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".statCalculator";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_NAME = "name";
	
	public String getName();
	
	public double calculate(IStatContext context);
}
