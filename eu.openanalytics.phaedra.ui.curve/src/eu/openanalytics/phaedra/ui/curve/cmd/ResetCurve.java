package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class ResetCurve extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<Curve> curves = SelectionUtils.getObjects(selection, Curve.class);
	
		if ((curves == null || curves.isEmpty()) && event.getTrigger() instanceof Event) {
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
				boolean confirm = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Reset Curve Settings?",
						"Are you sure you want to reset " + curves.size() + " curve(s) to their default settings?"
						+ "\nNote: the curves will be automatically re-fit.");
				if (!confirm) return null;
				
				CurveFitException fitException = null;
				int failCount = 0;
				int count = 0;
				for (Curve curve: curves) {
					count++;
					try {
						CurveFitService.getInstance().updateCurveSettings(curve, null);
						CurveFitService.getInstance().fitCurve(curve);
					} catch (CurveFitException e) {
						fitException = e;
						failCount++;
					}
				}
				
				if (fitException != null) {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Reset Failed", "Reset failed for " + failCount + "/" + count + " curve(s): " + fitException.getMessage());
				}
			}
		}
	
		return null;
	}
}