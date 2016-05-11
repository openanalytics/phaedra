package eu.openanalytics.phaedra.model.plate.compound;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.Activator;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public interface ICompoundInfoProvider {

	public static final String EXT_PT_ID = Activator.PLUGIN_ID + ".compoundInfoProvider";
	public static final String ATTR_CLASS = "class";
	
	public String getName();
	
	public List<String> getSupportedCompoundTypes();
	
	public List<CompoundInfo> getInfo(List<Compound> compounds);
}
