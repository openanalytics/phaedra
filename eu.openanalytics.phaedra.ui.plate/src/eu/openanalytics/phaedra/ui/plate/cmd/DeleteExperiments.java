package eu.openanalytics.phaedra.ui.plate.cmd;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class DeleteExperiments extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
		if (!experiments.isEmpty()) {
			boolean access = true;
			for (Experiment exp: experiments) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_DELETE, exp);
				if (!access) break;
			}
			if (access) {
				delete(experiments);
			}
		}
		return null;
	}
	
	private void delete(final List<Experiment> experiments) {
		Shell shell = Display.getDefault().getActiveShell();
		
		boolean confirm = MessageDialog.openQuestion(shell,
				"Delete Experiment(s)",
				"Are you sure you want to delete the selected Experiment(s)?");
		if (!confirm) return;
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Deleting experiments...", IProgressMonitor.UNKNOWN);
				for (Experiment exp: experiments) {
					PlateService.getInstance().deleteExperiment(exp);
				}
				monitor.done();
			}
		};
		try {
			new ProgressMonitorDialog(shell).run(true, false, runnable);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Delete Error", "Cannot delete experiment: " + e.getMessage());
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error while deleting experiment", e));
		}
	}
}
