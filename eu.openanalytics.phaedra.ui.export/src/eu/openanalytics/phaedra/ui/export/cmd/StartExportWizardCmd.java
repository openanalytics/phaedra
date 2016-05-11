package eu.openanalytics.phaedra.ui.export.cmd;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.export.wizard.ExportWizard;

public class StartExportWizardCmd extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		ISelection selection = page.getSelection();
		List<Experiment> experiments = getExperiments(selection);

		if (!experiments.isEmpty()) {
			boolean access = true;
			for (Experiment e: experiments) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_OPEN, e);
				if (!access) break;
			}
			if (access) {
				ExportWizard wizard = new ExportWizard(experiments);
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.create();
				dialog.open();
			}
		}
		return null;
	}

	private List<Experiment> getExperiments(ISelection selection) {
		return SelectionUtils.getObjects(selection, Experiment.class);
	}

	@Override
	public boolean isEnabled() {
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		Function<Experiment, ProtocolClass> function = e -> e.getProtocol().getProtocolClass();
		Collection<ProtocolClass> protocolClasses = getExperiments(selection).stream().map(function).collect(Collectors.toSet());
		return protocolClasses.size() == 1;
	}

}
