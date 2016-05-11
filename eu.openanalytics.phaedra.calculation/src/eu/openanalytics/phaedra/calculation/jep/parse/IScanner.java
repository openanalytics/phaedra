package eu.openanalytics.phaedra.calculation.jep.parse;

import eu.openanalytics.phaedra.calculation.Activator;

public interface IScanner {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".jepScanner";
	public final static String ATTR_CLASS = "class";
	
	public VarReference[] scan(JEPExpression expression, Object obj);
	
}