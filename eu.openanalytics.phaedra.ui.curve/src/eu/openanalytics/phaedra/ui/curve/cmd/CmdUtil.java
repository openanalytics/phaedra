package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public class CmdUtil {
	
	public static void executeCmd(String id, Object data) {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IHandlerService handlerService = part.getSite().getService(IHandlerService.class);
		Event cmdEvent = new Event();
		cmdEvent.data = data;
		try {
			handlerService.executeCommand(id, cmdEvent);
		} catch (Exception ex) {
			throw new RuntimeException("Command failed", ex);
		}
	}
	
	public static List<Compound> getCompounds(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		ISelection selection = page.getSelection();
		
		return SelectionUtils.getObjects(selection, Compound.class);
	}
	
}
