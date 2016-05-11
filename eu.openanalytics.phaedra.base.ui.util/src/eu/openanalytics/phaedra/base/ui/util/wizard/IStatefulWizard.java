package eu.openanalytics.phaedra.base.ui.util.wizard;

/**
 * A stateful wizard is a wizard that passes state
 * information between pages.
 */
public interface IStatefulWizard {

	public IWizardState getState();

}
