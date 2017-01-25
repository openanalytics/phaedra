package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.dialog.RunScriptDialog;

public class RunScript extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection) HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) {
			// Maybe an experiment selection was made rather than a plate selection.
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			plates = new ArrayList<Plate>();
			for (Experiment exp: experiments) plates.addAll(PlateService.getInstance().getPlates(exp));
		}
		if (!plates.isEmpty()) execute(plates);
		return null;
	}
	
	public static void execute(List<Plate> plates) {
		RunScriptDialog dialog = new RunScriptDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), plates);
		dialog.open();
	}
}