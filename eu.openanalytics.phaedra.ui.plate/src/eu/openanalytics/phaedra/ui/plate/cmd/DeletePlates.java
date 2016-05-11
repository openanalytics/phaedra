package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DeletePlates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (!plates.isEmpty()) {
			boolean access = true;
			for (Plate p: plates) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_DELETE, p);
				if (!access) break;
			}
			if (access) {
				delete(plates);
			}
		}
		return null;
	}
	
	private void delete(final List<Plate> plates) {
		boolean confirm = MessageDialog.openQuestion(
				Display.getDefault().getActiveShell(),
				"Delete Plate(s)",
				"Are you sure you want to delete the selected Plate(s)?");
		if (!confirm) return;
		
		Job deletePlatesJob = new Job("Deleting plate(s)...") {
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Deleting " + plates.size() + " plate(s)...", IProgressMonitor.UNKNOWN);
				PlateService.getInstance().deletePlates(plates);
				monitor.done();
				return Status.OK_STATUS;
			};
		};
		deletePlatesJob.setUser(true);
		deletePlatesJob.schedule();
	}
}
