package eu.openanalytics.phaedra.ui.curve;

import java.util.List;

import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.plate.compound.ICompoundView;
import eu.openanalytics.phaedra.model.plate.compound.WrappedCompoundView;
import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * Wraps a Compound with information about its groupings.
 */
public class CompoundWithGrouping extends WrappedCompoundView {
	
	
	private CurveGrouping grouping;
	
	
	public CompoundWithGrouping(ICompoundView delegate, CurveGrouping grouping) {
		super(delegate);
		this.grouping = grouping;
	}
	
	
	public CurveGrouping getGrouping() {
		return grouping;
	}
	
	@Override
	public List<Well> getWells() {
		return delegate.getWells();
	}
	
	@Override
	public int getWellCount() {
		return delegate.getWellCount();
	}
	
	
	@Override
	public int hashCode() {
		int hash = delegate.hashCode();
		hash = 31 * hash + grouping.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof CompoundWithGrouping) {
			CompoundWithGrouping other = (CompoundWithGrouping) obj;
			return (delegate.equals(other.delegate)
					&& grouping.equals(other.grouping));
		}
		return false;
	}
	
}
