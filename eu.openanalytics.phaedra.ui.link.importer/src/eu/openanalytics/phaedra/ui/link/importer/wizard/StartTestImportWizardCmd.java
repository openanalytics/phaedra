package eu.openanalytics.phaedra.ui.link.importer.wizard;

import org.eclipse.core.commands.ExecutionEvent;

import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class StartTestImportWizardCmd extends StartImportWizardCmd {

	@Override
	protected void configureState(ImportWizardState preconfiguredState, ExecutionEvent event) {
		// Put the import task in test mode.
		preconfiguredState.task.getParameters().put(DataCaptureTask.PARAM_TEST, true);
		
		super.configureState(preconfiguredState, event);
	}
	
}