package eu.openanalytics.phaedra.base.ui.util.wizard;

import org.eclipse.jface.wizard.IWizardPage;

public interface ISkippableWizardPage extends IWizardPage {

	public boolean isSkippable(IWizardState state);

}
