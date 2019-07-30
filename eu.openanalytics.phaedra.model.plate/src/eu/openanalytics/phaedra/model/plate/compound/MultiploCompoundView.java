package eu.openanalytics.phaedra.model.plate.compound;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * Wraps a Compound with information about its multiplo siblings.
 */
public class MultiploCompoundView extends BaseCompoundView {
	
	
	private Compound keyCompound;
	private List<Compound> compounds;
	
	private volatile List<Well> wells;
	
	
	public MultiploCompoundView(List<Compound> compounds, Compound keyCompound) {
		this.keyCompound = keyCompound;
		this.compounds = compounds;
	}
	
	
	@Override
	public List<Compound> getCompounds() {
		return compounds;
	}
	
	@Override
	public Compound getFirstCompound() {
		return keyCompound;
	}
	
	@Override
	public Experiment getExperiment() {
		return keyCompound.getPlate().getExperiment();
	}
	
	@Override
	public List<Plate> getPlates() {
		List<Plate> plates = new ArrayList<>(compounds.size());
		for (Compound compound : compounds) {
			plates.add(compound.getPlate());
		}
		return plates;
	}
	
	@Override
	public String getType() {
		return keyCompound.getType();
	}
	
	@Override
	public String getNumber() {
		return keyCompound.getNumber();
	}

	@Override
	public String getSaltform() {
		return keyCompound.getSaltform();
	}
	
	@Override
	public List<Well> getWells() {
		List<Well> wells = this.wells;
		if (wells == null) {
			wells = new ArrayList<>(compounds.size() * keyCompound.getWells().size());
			for (Compound compound : compounds) {
				wells.addAll(compound.getWells());
			}
			this.wells = wells;
 		}
		return wells;
	}
	
	@Override
	public int getWellCount() {
		return compounds.stream().mapToInt(comp -> comp.getWells().size()).sum();
	}
	
	
	@Override
	public int hashCode() {
		return 31 * keyCompound.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof MultiploCompoundView) {
			MultiploCompoundView other = (MultiploCompoundView) obj;
			return this.compounds.equals(other.compounds);
		}
		return false;
	}
}
