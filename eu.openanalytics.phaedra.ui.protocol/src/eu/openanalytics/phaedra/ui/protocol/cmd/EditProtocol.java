package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.protocol.dialog.EditProtocolDialog;

public class EditProtocol extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Protocol protocol = SelectionUtils.getFirstObject(selection, Protocol.class);
		execute(protocol);
		return null;
	}
	
	public static void execute(Protocol protocol) {
		if (protocol == null) return;
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOL_EDIT, protocol);
		if (access) {
			EditProtocolDialog dialog = new EditProtocolDialog(Display.getCurrent().getActiveShell(), protocol);
			dialog.open();
		}
	}

}