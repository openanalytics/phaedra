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
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.dialog.CreateProtocolDialog;

public class CreateProtocol extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		ProtocolClass protocolClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
		if (SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOL_CREATE, protocolClass)) {
			CreateProtocolDialog dialog = new CreateProtocolDialog(Display.getCurrent().getActiveShell(), protocolClass);
			dialog.open();
		}
		return null;
	}

}