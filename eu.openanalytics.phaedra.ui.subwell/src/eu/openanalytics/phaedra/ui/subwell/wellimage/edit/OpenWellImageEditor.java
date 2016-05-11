package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class OpenWellImageEditor extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
		if (plate == null) {
			if (event.getTrigger() instanceof Event) {
				Event e = (Event)event.getTrigger();
				if (e.data instanceof Plate) plate = (Plate)e.data;
			}
		}
		if (plate != null) showEditor(plate);
		return null;
	}
	
	public static void execute(Plate plate) {
		new OpenWellImageEditor().showEditor(plate);
	}
	
	private void showEditor(Plate plate) {
		if (!plate.isImageAvailable()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot open: no image available",
					"Cannot open the well image editor: this plate has no image attached to it.");
			return;
		}
		
		if (ClassificationService.getInstance().findSubWellClassificationFeatures(ProtocolUtils.getProtocolClass(plate)).isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot open: no classifications available",
					"Cannot open the well image editor: this plate's protocol class has no subwell classification features.");
			return;
		}

		IEditorInput input = new WellImageEditorInput(plate);
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(input, WellImageEditor.class.getName());
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}

}
