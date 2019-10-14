package eu.openanalytics.phaedra.ui.plate.calculation;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationMode;
import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRule;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.protocol.Activator;

public class RunHitCallingHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) plates = SelectionUtils.getObjects(selection, Experiment.class).stream()
				.flatMap(e -> PlateService.getInstance().getPlates(e).stream())
				.collect(Collectors.toList());
		if (!plates.isEmpty()) execute(plates);
		return null;
	}
	
	public static boolean execute(List<Plate> plates) {
		if (plates == null || plates.isEmpty()) return false;
		
		RunHitCallingDialog dialog = new RunHitCallingDialog(Display.getCurrent().getActiveShell(), plates) {
			@Override
			protected void okPressed() {
				try {
					for (Entry<HitCallRule, Double> entry: getCustomThresholds().entrySet()) {
						HitCallService.getInstance().saveCustomThresholds(plates, entry.getKey(), entry.getValue());
					}
					triggerRecalcJob(plates);
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to update hit call thresholds:", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		return (dialog.open() == Dialog.OK);
	}
	
	private static void triggerRecalcJob(List<Plate> plates) {
		Job recalcJob = new Job("Recalculating") {
			protected IStatus run(IProgressMonitor monitor) {
				int workload = plates.size();
				if (workload == 1) workload = IProgressMonitor.UNKNOWN;
				monitor.beginTask("Recalculating " + plates.size() + " plate(s)", workload);
				try {
					for (Plate plate: plates) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						monitor.subTask("Recalculating plate " + plate.getBarcode());
						CalculationService.getInstance().calculate(plate, CalculationMode.NORMAL);
						monitor.worked(1);
					}
				} catch (CalculationException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e.getCause());
				}
				return Status.OK_STATUS;
			};
		};
		recalcJob.setUser(true);
		recalcJob.schedule();
	}
	
}