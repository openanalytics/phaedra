package eu.openanalytics.phaedra.model.plate.compound;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public class WrappedCompoundView extends BaseCompoundView {
	
	
	protected final ICompoundView delegate;
	
	
	public WrappedCompoundView(ICompoundView delegate) {
		this.delegate = delegate;
	}
	
	
	public ICompoundView getUnderlyingView() {
		return delegate;
	}
	
	@Override
	public List<Compound> getCompounds() {
		return delegate.getCompounds();
	}
	
	@Override
	public Compound getFirstCompound() {
		return delegate.getFirstCompound();
	}
	
	@Override
	public Experiment getExperiment() {
		return delegate.getExperiment();
	}
	
	@Override
	public List<Plate> getPlates() {
		return delegate.getPlates();
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
	public List<Well> getWells() {
		return delegate.getWells();
	}
	
	@Override
	public int getWellCount() {
		return delegate.getWellCount();
	}
	
}
