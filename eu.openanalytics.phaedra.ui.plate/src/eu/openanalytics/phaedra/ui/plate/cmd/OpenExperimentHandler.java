package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;


public class OpenExperimentHandler extends AbstractHandler {
	
	
	public OpenExperimentHandler() {
	}
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final SecurityService securityService = SecurityService.getInstance();
		final PlateService plateService = PlateService.getInstance();
		
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		final List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class, false);
		for (Iterator<Experiment> iter = experiments.iterator(); iter.hasNext();) {
			final Experiment experiment = iter.next();
			if (experiment.isClosed()) {
				if (!securityService.checkWithDialog(Permissions.EXPERIMENT_EDIT, experiment)) {
					return null;
				}
			}
			else {
				iter.remove();
			}
		}
		
		for (final Experiment experiment : experiments) {
			experiment.setClosed(false);
			plateService.updateExperiment(experiment);
		}
		return null;
	}
	
}
