package eu.openanalytics.phaedra.model.plate.util;

import org.eclipse.ui.IStartup;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

/**
 * Hook that makes sure all experiments in a protocol are
 * deleted before the protocol itself is deleted.
 */
public class ProtocolDeleteHook implements IStartup {

	private IModelEventListener eventListener;
	
	@Override
	public void earlyStartup() {

		eventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				if (event.type == ModelEventType.ObjectAboutToBeRemoved) {
					if (event.source instanceof Protocol) {
						Protocol p = (Protocol)event.source;
						deleteExperiments(p);
					} else if (event.source instanceof ProtocolClass) {
						ProtocolClass pc = (ProtocolClass)event.source;
						deleteProtocols(pc);
					}
				}
			}
		};
		
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private void deleteProtocols(ProtocolClass pc) {
		for (Protocol p: ProtocolService.getInstance().getProtocols(pc)) {
			deleteExperiments(p);
			ProtocolService.getInstance().deleteProtocol(p);
		}
	}
	
	private void deleteExperiments(Protocol p) {
		for (Experiment exp: PlateService.getInstance().getExperiments(p)) {
			PlateService.getInstance().deleteExperiment(exp);
		}
	}
}
