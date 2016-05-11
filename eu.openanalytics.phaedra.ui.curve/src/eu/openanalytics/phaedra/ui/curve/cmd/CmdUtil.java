package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.MultiploCompound;

public class CmdUtil {
	
	public static void executeCmd(String id, Object data) {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IHandlerService handlerService = (IHandlerService)part.getSite().getService(IHandlerService.class);
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
		
		Set<Compound> compounds = new HashSet<>();
		for (Compound c: SelectionUtils.getObjects(selection, Compound.class)) {
			if (c instanceof MultiploCompound) compounds.addAll(((MultiploCompound) c).getCompounds());
			else if (c instanceof CompoundWithGrouping) compounds.add(((CompoundWithGrouping) c).getDelegate());
			else compounds.add(c);
		}
		
		return new ArrayList<>(compounds);
	}
	
}
