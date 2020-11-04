package eu.openanalytics.phaedra.calculation.norm;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;
import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;


public interface INormalizer {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".normalizer";
	public final static String ATTR_CLASS = "class";
	
	public String getId();
	
	public FormulaDescriptor getFormulaDescriptor();
	
	public NormalizedGrid calculate(NormalizationKey key) throws NormalizationException;
}
