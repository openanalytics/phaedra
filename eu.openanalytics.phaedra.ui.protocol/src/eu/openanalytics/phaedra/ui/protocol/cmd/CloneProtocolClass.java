package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class CloneProtocolClass extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		ProtocolClass protocolClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
		if (protocolClass == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "No Protocol Class selected", "No Protocol Class selected");
		} else {
			boolean confirm = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
					"Clone Protocol Class", "Are you sure you want to clone the protocol class " + protocolClass.toString() + "?"
					+ "\nA clone contains the same features, subwell features, image channels and other settings as the original.");
			if (!confirm) return null;
			ProtocolService.getInstance().cloneProtocolClass(protocolClass);
		}
		return null;
	}

}