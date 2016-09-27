package eu.openanalytics.phaedra.model.curve.util;

import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof Curve) {
			Curve curve = (Curve)adaptableObject;
			
			if (adapterType == Feature.class) return adapterType.cast(curve.getFeature());
			
			if (curve.getCompounds() == null || curve.getCompounds().isEmpty()) return null;
			Compound c = curve.getCompounds().get(0);
			
			if (adapterType == Compound.class) return adapterType.cast(c);
			else if (adapterType == Plate.class) return adapterType.cast(c.getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(c.getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(c.getPlate().getExperiment().getProtocol().getProtocolClass());
		}

		if (adaptableObject instanceof CRCurve) {
			CRCurve crCurve = (CRCurve) adaptableObject;
			Curve curve = CurveFitService.getInstance().getCurve(crCurve.getId());
			if (curve == null) return null;
			return getAdapter(curve, adapterType);
		}
		
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] {
				Feature.class,
				Compound.class,
				Plate.class,
				Experiment.class,
				Protocol.class,
				ProtocolClass.class
		};
	}

}
