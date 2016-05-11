package eu.openanalytics.phaedra.ui.export.subwell.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.export.subwell.wizard.SubWellDataExportWizard;

public class ExportSubWellDataCmd extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
		
		if (wells.isEmpty()) {
			List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
			for (Plate p : plates) wells.addAll(p.getWells());
		}
		
		if (wells.isEmpty()) {
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			for (Experiment exp: experiments) {
				List<Plate> plates = PlateService.getInstance().getPlates(exp);
				for (Plate p : plates) wells.addAll(p.getWells());
			}
		}
		
		if (!wells.isEmpty()) {
			Shell shell = Display.getCurrent().getActiveShell();
			WizardDialog wizardDialog = new WizardDialog(shell, new SubWellDataExportWizard(wells));
			wizardDialog.open();
		}
		
		return null;
	}

}