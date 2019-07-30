package eu.openanalytics.phaedra.model.plate.compound;

import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public class SingleCompoundView extends BaseCompoundView {
	
	
	private final Compound compound;
	
	
	public SingleCompoundView(Compound compound) {
		this.compound = compound;
	}
	
	
	@Override
	public List<Compound> getCompounds() {
		return Collections.singletonList(compound);
	}
	
	@Override
	public Compound getFirstCompound() {
		return compound;
	}
	
	@Override
	public Experiment getExperiment() {
		return compound.getPlate().getExperiment();
	}
	
	@Override
	public List<Plate> getPlates() {
		return Collections.singletonList(compound.getPlate());
	}
	
	@Override
	public String getType() {
		return compound.getType();
	}
	
	@Override
	public String getNumber() {
		return compound.getNumber();
	}
	
	@Override
	public String getSaltform() {
		return compound.getSaltform();
	}
	
	@Override
	public List<Well> getWells() {
		return compound.getWells();
	}
	
	@Override
	public int getWellCount() {
		return compound.getWells().size();
	}
	
	
	@Override
	public int hashCode() {
		return compound.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof SingleCompoundView) {
			SingleCompoundView other = (SingleCompoundView) obj;
			return (compound.getId() == other.compound.getId());
		}
		return false;
	}
	
}
