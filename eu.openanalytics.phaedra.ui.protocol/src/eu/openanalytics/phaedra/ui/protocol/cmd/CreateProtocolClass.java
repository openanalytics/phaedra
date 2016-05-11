package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.editor.ProtocolClassEditor;
import eu.openanalytics.phaedra.ui.protocol.editor.ProtocolClassEditorInput;

public class CreateProtocolClass extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOLCLASS_CREATE, null);
		if (access) {
			ProtocolClass protocolClass = ProtocolService.getInstance().createProtocolClass();
			showEditor(protocolClass);
		}
		return null;
	}
	
	private void showEditor(ProtocolClass protocolClass) {
		IEditorInput input = new ProtocolClassEditorInput(protocolClass, true);
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(input, ProtocolClassEditor.class.getName());
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}

}
