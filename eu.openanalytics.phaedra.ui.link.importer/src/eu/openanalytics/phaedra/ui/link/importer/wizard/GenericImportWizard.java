package eu.openanalytics.phaedra.ui.link.importer.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.ui.link.importer.wizard.page.ImportTypePage;
import eu.openanalytics.phaedra.ui.link.importer.wizard.page.MapPlatesPage;
import eu.openanalytics.phaedra.ui.link.importer.wizard.page.SelectExperimentPage;
import eu.openanalytics.phaedra.ui.link.importer.wizard.page.SelectSourcePage;


public class GenericImportWizard extends BaseStatefulWizard {
	
	public GenericImportWizard() {
		setWindowTitle("Generic Importer");
		state = new ImportWizardState();
	}
	
	public GenericImportWizard(ImportWizardState preconfiguredState) {
		this();
		setPreconfiguredState(preconfiguredState);
	}
	
	@Override
	public void addPages() {
		addPage(new ImportTypePage());
		addPage(new SelectSourcePage());
		addPage(new SelectExperimentPage());
		addPage(new MapPlatesPage());
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		boolean createNewPlates = ((ImportWizardState)state).task.createNewPlates;
		IWizardPage nextPage = super.getNextPage(page);
		if (createNewPlates && nextPage instanceof MapPlatesPage) {
			return null;
		}
		return nextPage;
	}

	@Override
	public boolean canFinish() {
		boolean createNewPlates = ((ImportWizardState)state).task.createNewPlates;
		for (int i = 0; i < getPages().length; i++) {
			IWizardPage page = getPages()[i];
			if (page instanceof MapPlatesPage && createNewPlates) continue;
			if (!page.isPageComplete()) return false;
		}
		return true;
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		
		ImportTask task = ((ImportWizardState)state).task;
		String userName = SecurityService.getInstance().getCurrentUserName();
		task.userName = userName;
		
		return checkFinish(
				ImportService.getInstance().startJob(task) );
	}
	
	private boolean checkFinish(final IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			MessageDialog.openError(getShell(), "Import", status.getMessage());
		}
		return (status.getSeverity() < IStatus.ERROR);
	}


	public static class ImportWizardState implements IWizardState {
		
		public ImportWizardState() {
			task = new ImportTask();
		}
		
		public ImportTask task;
	}
}
