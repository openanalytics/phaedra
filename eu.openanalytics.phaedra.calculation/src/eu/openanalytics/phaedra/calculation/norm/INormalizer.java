package eu.openanalytics.phaedra.calculation.norm;

import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;


public interface INormalizer {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".normalizer";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public String getId();
	
	public String getDescription();
	
	public NormalizedGrid calculate(NormalizationKey key) throws NormalizationException;
}
