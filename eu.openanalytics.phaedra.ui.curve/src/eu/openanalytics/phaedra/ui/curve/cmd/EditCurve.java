package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.ui.curve.edit.EditCurveDialog;

public class EditCurve extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Curve> curves = SelectionUtils.getObjects(selection, Curve.class);
	
		if (curves.isEmpty() && event.getTrigger() instanceof Event) {
			Object data = ((Event)event.getTrigger()).data;
			if (data instanceof Curve) curves = Lists.newArrayList((Curve)data);
		}
		
		if (curves != null && !curves.isEmpty()) {
			boolean access = true;
			for (Curve curve: curves) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, curve);
				if (!access) break;
			}
			if (access) {			
				EditCurveDialog dialog = new EditCurveDialog(Display.getDefault().getActiveShell(),	curves.toArray(new Curve[curves.size()]));
				dialog.open();
			}
		}
	
		return null;
	}
}