package eu.openanalytics.phaedra.model.plate.compound;

import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public abstract class BaseCompoundView extends PlatformObject implements ICompoundView {
	
	
	public BaseCompoundView() {
	}
	
	
	@Override
	public <T> T getAdapter(Class<T> type) {
		if (type == ProtocolClass.class) {
			return (T)getExperiment().getProtocol().getProtocolClass();
		}
		if (type == Protocol.class || type == IOwnedObject.class) {
			return (T)getExperiment().getProtocol();
		}
		if (type == Experiment.class) {
			return (T)getExperiment();
		}
		if (type == Plate.class) {
			return (T)getFirstCompound().getPlate();
		}
		if (type == Compound.class) {
			return (T)getFirstCompound();
		}
		return super.getAdapter(type);
	}
	
	@Override
	public <T> List<T> getAdapterList(Class<T> type) {
		if (type == Plate.class) {
			return (List<T>) getPlates();
		}
		if (type == Compound.class) {
			return (List<T>) getCompounds();
		}
		if (type == Well.class) {
			return (List<T>) getWells();
		}
		return null;
	}
	
	
	@Override
	public String toString() {
		return getType() + " " + getNumber();
	}
	
}
