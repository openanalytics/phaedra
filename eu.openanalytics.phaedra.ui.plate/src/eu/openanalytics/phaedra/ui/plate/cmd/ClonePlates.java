package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class ClonePlates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		execute(plates);
		return null;
	}

	public static void execute(List<Plate> plates) {
		if (plates == null) return;
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, plates);
		if (access) {
			Job cloneJob = new Job("Clone plate(s)") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						// Make possible to copy a selection of multiple plates
						plates.stream().forEach(plate -> PlateService.getInstance().clonePlate(plate, monitor));
					} catch (Exception e) {
						return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
					}
					return Status.OK_STATUS;
				}
			};
			cloneJob.setUser(true);
			cloneJob.schedule();
		}
	}
}
