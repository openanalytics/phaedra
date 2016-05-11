package eu.openanalytics.phaedra.model.subwell.util;

import java.util.BitSet;

import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;

public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof SubWellItem && ((SubWellItem)adaptableObject).getWell() != null) {
			SubWellItem item = (SubWellItem) adaptableObject;
			if (adapterType == Well.class || adapterType == IValueObject.class) return adapterType.cast(item.getWell());
			else if (adapterType == Compound.class) return adapterType.cast(item.getWell().getCompound());
			else if (adapterType == Plate.class) return adapterType.cast(item.getWell().getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(item.getWell().getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(item.getWell().getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(item.getWell().getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(item.getWell().getPlate().getExperiment().getProtocol().getProtocolClass());
			else if (adapterType == SubWellSelection.class) {
				BitSet bits = new BitSet();
				bits.set(item.getIndex());
				return adapterType.cast(new SubWellSelection(item.getWell(), bits));
			}
		}
		
		if (adaptableObject instanceof SubWellSelection && ((SubWellSelection)adaptableObject).getWell() != null) {
			SubWellSelection sel = (SubWellSelection) adaptableObject;
			Well well = sel.getWell();
			if (adapterType == Well.class || adapterType == IValueObject.class) return adapterType.cast(well);
			else if (adapterType == Compound.class) return adapterType.cast(well.getCompound());
			else if (adapterType == Plate.class) return adapterType.cast(well.getPlate());
			else if (adapterType == Experiment.class) return adapterType.cast(well.getPlate().getExperiment());
			else if (adapterType == Protocol.class) return adapterType.cast(well.getPlate().getExperiment().getProtocol());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(well.getPlate().getExperiment().getProtocol());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(well.getPlate().getExperiment().getProtocol().getProtocolClass());
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
