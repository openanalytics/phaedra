package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationMode;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class RecalculatePlates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) {
			// Maybe an experiment selection was made rather than a plate selection.
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			plates = new ArrayList<Plate>();
			for (Experiment exp: experiments) {
				plates.addAll(PlateService.getInstance().getPlates(exp));
			}
		}
		if (!plates.isEmpty()) {
			boolean access = true;
			for (Plate p: plates) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_CALCULATE, p);
				if (!access) break;
			}
			if (access) {
				recalc(plates);
			}
		}
		return null;
	}
	
	private void recalc(final List<Plate> plates) {
		ConfirmDialog dialog = new ConfirmDialog(Display.getDefault().getActiveShell(), plates.size());
		int retCode = dialog.open();
		if (retCode == Window.CANCEL) return;
		
		final CalculationMode mode = dialog.getSelectedMode();
		
		Job recalcJob = new Job("Recalculating") {
			protected IStatus run(IProgressMonitor monitor) {
				int workload = plates.size();
				if (workload == 1) workload = IProgressMonitor.UNKNOWN;
				monitor.beginTask("Recalculating " + plates.size() + " plate(s)", workload);
				try {
					for (Plate plate: plates) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						monitor.subTask("Recalculating plate " + plate.getBarcode());
						CalculationService.getInstance().calculate(plate, mode);
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
	
	private static class ConfirmDialog extends TitleAreaDialog {

		private int plateCount;
		private CalculationMode selectedMode;
		
		public ConfirmDialog(Shell parentShell, int plateCount) {
			super(parentShell);
			this.plateCount = plateCount;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite outerArea = (Composite)super.createDialogArea(parent);
			Composite area = new Composite(outerArea, SWT.NONE);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(area);

			Label lbl = new Label(area, SWT.NONE);
			lbl.setText("Calculation mode:");
			
			for (CalculationMode mode: CalculationMode.values()) {
				Button btn = new Button(area, SWT.RADIO);
				btn.setText(mode.getLabel() + ": " + mode.getDescription());
				btn.setData(mode);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						selectedMode = (CalculationMode)e.widget.getData();
					}
				});
				if (mode == CalculationMode.NORMAL) {
					btn.setSelection(true);
					selectedMode = mode;
				}
			}
			
			setMessage(plateCount + " plate(s) will be recalculated. Please select a calculation mode below.");
			setTitle("Recalculate Plates");
			return area;
		}
		
		public CalculationMode getSelectedMode() {
			return selectedMode;
		}
	}
}
