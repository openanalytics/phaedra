package eu.openanalytics.phaedra.model.plate.compound;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import eu.openanalytics.phaedra.base.util.IListAdaptable;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;


/**
 * A view on a compound. It can be a single {@link Compound}, but also pool
 * multiple compounds of the same type (MultiploCompound) or filter properties of
 * compounds (CompoundWithGrouping).
 */
public interface ICompoundView extends IAdaptable, IListAdaptable {
	
	
	List<Compound> getCompounds();
	Compound getFirstCompound();
	
	Experiment getExperiment();
	List<Plate> getPlates();
	
	String getType();
	
	String getNumber();
	
	String getSaltform();
	
	List<Well> getWells();
	int getWellCount();
	
}
