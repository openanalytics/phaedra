package eu.openanalytics.phaedra.ui.export.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class BaseExportWizardPage extends WizardPage implements IExportWizardPage {

	boolean firstShow;
	
	protected BaseExportWizardPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		
		firstShow = true;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			pageAboutToShow(firstShow);
			firstShow = false;
		}
		super.setVisible(visible);
	}
	
	@Override
	public void collectSettings() {
		// Default: no settings changed.
	}

	@Override
	public void createControl(Composite parent) {
		// Default: empty page.
	}

	protected void pageAboutToShow(boolean firstTime) {
		// Default: take no action.
	}
	
	protected void checkPageComplete() {
		setPageComplete(validateSettings());
	}
	
	protected boolean validateSettings() {
		return true;
	}
	
	@Override
	public void saveDialogSettings() {
	}
	
}
