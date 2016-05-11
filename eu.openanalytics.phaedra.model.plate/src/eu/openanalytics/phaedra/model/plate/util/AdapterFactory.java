package eu.openanalytics.phaedra.model.plate.util;

import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof FeatureValue) {
			FeatureValue fv = (FeatureValue) adaptableObject;
			if (adapterType == Feature.class) return adapterType.cast(fv.getFeature());
			if (fv.getWell() == null) return null;
			if (adapterType == Well.class) return adapterType.cast(fv.getWell());
			else if (adapterType == Compound.class) return adapterType.cast(fv.getWell().getCompound());
			else if (adapterType == Plate.class) return adapterType.cast(fv.getWell().getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(fv.getWell().getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(fv.getWell().getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(fv.getWell().getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(fv.getWell().getPlate().getExperiment().getProtocol().getProtocolClass());
		}
		if (adaptableObject instanceof Well) {
			Well w = (Well) adaptableObject;
			if (adapterType == Compound.class) return adapterType.cast(w.getCompound());
			else if (adapterType == Plate.class) return adapterType.cast(w.getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(w.getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(w.getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(w.getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(w.getPlate().getExperiment().getProtocol().getProtocolClass());
		}
		if (adaptableObject instanceof Compound) {
			Compound c = (Compound) adaptableObject;
			if (adapterType == Plate.class) return adapterType.cast(c.getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(c.getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol().getProtocolClass());
		}
		if (adaptableObject instanceof Plate) {
			Plate p = (Plate) adaptableObject;
			if (adapterType == Experiment.class) return adapterType.cast(p.getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(p.getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(p.getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(p.getExperiment().getProtocol().getProtocolClass());
		}
		if (adaptableObject instanceof Experiment) {
			Experiment e = (Experiment) adaptableObject;
			if (adapterType == Protocol.class) return adapterType.cast(e.getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(e.getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(e.getProtocol().getProtocolClass());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { 
				Feature.class,
				Well.class,
				Plate.class,
				Experiment.class,
				Protocol.class,
				ProtocolClass.class
		};
	}

}
