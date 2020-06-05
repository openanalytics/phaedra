package eu.openanalytics.phaedra.ui.link.importer.wizard;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.Activator;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class StartImportWizardCmd extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (ProtocolService.getInstance().getProtocols().isEmpty()) {
			String msg = "Cannot import data: no protocols have been defined."
					+ "\nPlease use the Protocol Wizard (File > Create New Protocol) to define a protocol first.";
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "No protocols defined", null, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));
			return null;
		}
		
		ImportWizardState preconfiguredState = new ImportWizardState();
		configureState(preconfiguredState, event);

		// Start the import wizard
		ImportWizard wizard = new ImportWizard(preconfiguredState);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.setPageSize(500, 420);
		dialog.create();
		dialog.open();
		
		return null;
	}

	protected void configureState(ImportWizardState preconfiguredState, ExecutionEvent event) {
		// Pre-select an experiment, use the first one found from the list of selected experiments or plates
		if (preconfiguredState.task.targetExperiment == null) {
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			Experiment activeExp = getExperimentFromSelection(selection);

			if (activeExp == null) {
				activeExp = getExperimentFromCurrentSites();
			}
			
			// TODO: An open plate list view without selection should be able to return the currently opened experiment.
			
			if (activeExp != null && !activeExp.isClosed()) {
				preconfiguredState.task.targetExperiment = activeExp;
			}
		}	
	}

	/**
	 * Try to get Experiment from given selection.
	 * Null if no experiment was found.
	 * @param selection
	 * @return
	 */
	private Experiment getExperimentFromSelection(ISelection selection) {
		List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
		// Return Experiment from Experiment Selection.
		if (!experiments.isEmpty()) return experiments.get(0);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		// Return Experiment based on Plate Selection.
		if (!plates.isEmpty()) return plates.get(0).getExperiment();
		Protocol protocol = SelectionUtils.getFirstObject(selection, Protocol.class);
		// Return first Experiment of selected Protocol.
		if (protocol != null) {
			experiments = PlateService.getInstance().getExperiments(protocol);
			if (!experiments.isEmpty()) return experiments.get(0);
			else return PlateService.getInstance().createExperiment(protocol);
		}

		return null;
	}

	private Experiment getExperimentFromCurrentSites() {
		Experiment exp = null;
		IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
		for (IWorkbenchPage page : pages) {
			exp = getExperimentFromPartReferences(page.getEditorReferences());
			if (exp != null) return exp;
			exp = getExperimentFromPartReferences(page.getViewReferences());
			if (exp != null) return exp;
		}
		return exp;
	}

	private Experiment getExperimentFromPartReferences(IWorkbenchPartReference[] partReferences) {
		for (IWorkbenchPartReference partReference : partReferences) {
			ISelection selection = getSelectionFromPart(partReference.getPart(true));
			Experiment exp = getExperimentFromSelection(selection);
			if (exp != null) return exp;
		}
		return null;
	}

	/**
	 * Try to get Selection for given WorkbenchPart.
	 * Null if no selection.
	 * @param part
	 * @return
	 */
	private ISelection getSelectionFromPart(IWorkbenchPart part) {
		if (part != null) {
			IWorkbenchPartSite site = part.getSite();
			if (site != null) {
				ISelectionProvider selectionProvider = site.getSelectionProvider();
				if (selectionProvider != null) return selectionProvider.getSelection();
			}
		}
		return null;
	}
	
}