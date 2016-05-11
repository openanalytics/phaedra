package eu.openanalytics.phaedra.ui.link.platedef.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.ui.link.platedef.Activator;
import eu.openanalytics.phaedra.ui.link.platedef.template.PlateTemplateEditor;
import eu.openanalytics.phaedra.ui.link.platedef.template.PlateTemplateEditorInput;

public class CreatePlateTemplate extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (ProtocolService.getInstance().getProtocols().isEmpty()) {
			String msg = "Cannot create template: no protocols have been defined."
					+ "\nPlease use the Protocol Wizard (File > Create New Protocol) to define a protocol first.";
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "No protocols defined", null, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));
			return null;
		}
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		try {
			PlateTemplate template = new PlateTemplate();
			template.fillBlank();
			template.setCreator(SecurityService.getInstance().getCurrentUserName());
			PlateTemplateEditorInput input = new PlateTemplateEditorInput();
			input.setPlateTemplate(template);
			input.setNewTemplate(true);
			IEditorPart editor = page.openEditor(input, PlateTemplateEditor.class.getName());
			((PlateTemplateEditor)editor).setDirty(true);
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to launch the editor " + PlateTemplateEditor.class.getName(), e);
		}
		return null;
	}

}
