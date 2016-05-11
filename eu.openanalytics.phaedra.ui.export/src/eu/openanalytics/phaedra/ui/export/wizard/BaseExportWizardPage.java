package eu.openanalytics.phaedra.ui.export.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.export.core.ExportSettings;

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
			ExportWizard wiz = (ExportWizard)getWizard();
			pageAboutToShow(wiz.getSettings(), firstShow);
			firstShow = false;
		}
		super.setVisible(visible);
	}
	
	@Override
	public void collectSettings(ExportSettings settings) {
		// Default: no settings changed.
	}

	@Override
	public void createControl(Composite parent) {
		// Default: empty page.
	}

	protected void pageAboutToShow(ExportSettings settings, boolean firstTime) {
		// Default: take no action.
	}
}
