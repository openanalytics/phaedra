package eu.openanalytics.phaedra.ui.export.wizard;

public interface IExportWizardPage {

	/**
	 * Update export settings UI -> model
	 */
	public void collectSettings();
	
	/**
	 * Save dialog settings, called on OK/Finish
	 */
	public void saveDialogSettings();
	
}
