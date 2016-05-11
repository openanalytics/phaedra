package eu.openanalytics.phaedra.base.ui.util.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;


public abstract class BaseStatefulWizard extends Wizard implements IStatefulWizard {

	protected IWizardState state;

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// Collect the state from the previous page
		if (page instanceof IStatefulWizardPage) {
			((IStatefulWizardPage)page).collectState(state);
		}

		IWizardPage nextPage = super.getNextPage(page);
		if (nextPage instanceof ISkippableWizardPage) {
			boolean isSkippable = ((ISkippableWizardPage) nextPage).isSkippable(state);
			if (isSkippable) nextPage = getNextPage(nextPage);
		}
		return nextPage;
	}

	public void setPreconfiguredState(IWizardState state) {
		this.state = state;
	}

	@Override
	public IWizardState getState() {
		return state;
	}

	@Override
	public boolean performFinish() {
		for (IWizardPage page: getPages()) {
			if (page != null && page instanceof IStatefulWizardPage) ((IStatefulWizardPage)page).collectState(state);
		}
		return true;
	}
}
