package eu.openanalytics.phaedra.base.ui.util.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class BaseStatefulWizardPage extends WizardPage implements IStatefulWizardPage {

	private boolean firstTime;

	protected BaseStatefulWizardPage(String pageName) {
		super(pageName);
		firstTime = true;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			IStatefulWizard wiz = (IStatefulWizard)getWizard();
			applyState(wiz.getState(), firstTime);
			firstTime = false;
		}
		super.setVisible(visible);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		// Default impl: take no action.
	}

	@Override
	public void collectState(IWizardState state) {
		// Default impl: no settings changed.
	}

	@Override
	public void createControl(Composite parent) {
		// Default impl: empty page.
	}

}
