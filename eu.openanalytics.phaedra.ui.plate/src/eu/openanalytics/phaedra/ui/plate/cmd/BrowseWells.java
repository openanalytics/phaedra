package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class BrowseWells extends AbstractHandler {

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
		
		if (plates.isEmpty()) {
			// Maybe the command was called in a non-default way.
			if (event.getTrigger() instanceof Event) {
				Event e = (Event)event.getTrigger();
				if (e.data instanceof Plate) {
					plates.add((Plate)e.data);
				}
			}
		}
		
		if (!plates.isEmpty()) EditorFactory.getInstance().openEditor(plates);
		
		return null;
	}
	
	public static void execute(Plate plate) {
		EditorFactory.getInstance().openEditor(plate);
	}
	
	public static void execute(Well well) {
		IEditorPart editor = EditorFactory.getInstance().openEditor(well.getPlate());
		if (editor != null && editor.getEditorSite().getSelectionProvider() != null) {
			editor.getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(well));
		}
	}
}
