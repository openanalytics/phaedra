package eu.openanalytics.phaedra.ui.export.cmd;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.export.wizard.plate.ExportPlateTableWizard;

public class ExportPlateTableCmd extends AbstractHandler {
	
	@Override
	public boolean isEnabled() {
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getSelection();
		List<Experiment> experiments = getExperiments(selection);
		if (experiments.isEmpty()) {
			return false;
		}
		Collection<ProtocolClass> protocolClasses = experiments.stream()
				.map((e) -> e.getProtocol().getProtocolClass())
				.collect(Collectors.toSet());
		return (protocolClasses.size() == 1);
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		ISelection selection = window.getActivePage().getSelection();
		
		List<Experiment> experiments = getExperiments(selection);
		
		for (Experiment exp : experiments) {
			if (!SecurityService.getInstance().checkWithDialog(Permissions.PLATE_OPEN, exp)) {
				return null;
			}
		}
		
		if (!experiments.isEmpty()) {
			WizardDialog dialog = new WizardDialog(window.getShell(),
					new ExportPlateTableWizard(experiments));
			dialog.open();
		}
		return null;
	}
	
	private List<Experiment> getExperiments(ISelection selection) {
		return SelectionUtils.getObjects(selection, Experiment.class);
	}
	
}
