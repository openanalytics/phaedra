package eu.openanalytics.phaedra.ui.curve;

import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * Wraps a Compound with information about its multiplo siblings.
 * Note that this is only meant to be used as a read-only object in UI components (e.g. NatTables).
 * Do not attempt to modify or save instances of this object!
 */
public class MultiploCompound extends Compound {

	private static final long serialVersionUID = -3140625666817048509L;

	private Compound delegate;
	private List<Compound> compounds;
	
	public MultiploCompound(Compound delegate, List<Compound> compounds) {
		this.delegate = delegate;
		this.compounds = compounds;
	}
	
	public List<Compound> getCompounds() {
		return compounds;
	}
	
	public int getSampleCount() {
		return getCompounds().stream().mapToInt(comp -> comp.getWells().size()).sum();
	}
	
	@Override
	public long getId() {
		return delegate.getId();
	}
	
	@Override
	public Plate getPlate() {
		return delegate.getPlate();
	}
	
	@Override
	public String getType() {
		return delegate.getType();
	}
	
	@Override
	public String getNumber() {
		return delegate.getNumber();
	}

	@Override
	public String getSaltform() {
		return delegate.getSaltform();
	}
	
	@Override
	public String getDescription() {
		return delegate.getDescription();
	}
	
	@Override
	public List<Well> getWells() {
		return delegate.getWells();
	}
	
	@Override
	public Date getValidationDate() {
		return delegate.getValidationDate();
	}
	
	@Override
	public int getValidationStatus() {
		return delegate.getValidationStatus();
	}
	
	@Override
	public String getValidationUser() {
		return delegate.getValidationUser();
	}
	
	@Override
	public String toString() {
		return getType() + " " + getNumber();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (getId() ^ (getId() >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof Compound)) return false;
		Compound other = (Compound) obj;
		if (getId() != other.getId()) return false;
		return true;
	}
}