package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class BrowseExperiments extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Protocol protocol = SelectionUtils.getFirstObject(selection, Protocol.class);
		if (protocol != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOL_OPEN, protocol);
			if (access) showEditor(protocol);
		}
		return null;
	}
	
	public static void execute(Protocol protocol) {
		new BrowseExperiments().showEditor(protocol);
	}
	
	private void showEditor(final Protocol protocol) {
		EditorFactory.getInstance().openEditor(protocol);
	}
}
