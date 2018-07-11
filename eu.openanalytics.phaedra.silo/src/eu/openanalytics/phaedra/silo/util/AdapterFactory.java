package eu.openanalytics.phaedra.silo.util;

import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedPersonalObject;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof SiloDatasetColumn) {
			SiloDatasetColumn col = (SiloDatasetColumn) adaptableObject;
			if (adapterType == SiloDataset.class) return adapterType.cast(col.getDataset());
			else if (adapterType == Silo.class) return adapterType.cast(col.getDataset().getSilo());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(col.getDataset().getSilo().getProtocolClass());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(col.getDataset().getSilo());
			else if (adapterType == IOwnedPersonalObject.class) return adapterType.cast(col.getDataset().getSilo());
		}
		if (adaptableObject instanceof SiloDataset) {
			SiloDataset ds = (SiloDataset) adaptableObject;
			if (adapterType == Silo.class) return adapterType.cast(ds.getSilo());
			else if (adapterType == ProtocolClass.class) return adapterType.cast(ds.getSilo().getProtocolClass());
			else if (adapterType == IOwnedObject.class) return adapterType.cast(ds.getSilo());
			else if (adapterType == IOwnedPersonalObject.class) return adapterType.cast(ds.getSilo());
		}
		if (adaptableObject instanceof Silo) {
			Silo silo = (Silo) adaptableObject;
			if (adapterType == ProtocolClass.class) return adapterType.cast(silo.getProtocolClass());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { 
				Silo.class,
				SiloDataset.class,
				SiloDatasetColumn.class,
				ProtocolClass.class
		};
	}

}
