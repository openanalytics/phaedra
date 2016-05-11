package eu.openanalytics.phaedra.base.ui.util.wizard;

/**
 * A stateful wizard page is a page whose contents may
 * rely on the current state of a wizard, e.g. the information
 * provided or calculated in previous pages.
 * The state created by this page is then made available to any
 * subsequent pages.
 */
public interface IStatefulWizardPage {

	public void applyState(IWizardState state, boolean firstTime);

	public void collectState(IWizardState state);

}
