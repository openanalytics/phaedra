package eu.openanalytics.phaedra.ui.columbus.protocolwizard;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.ui.columbus.protocolwizard.ColumbusProtocolWizard.WizardState;
import eu.openanalytics.phaedra.ui.link.importer.addfeature.NewFeatureTableViewer;

public class SubWellFeaturePage extends BaseStatefulWizardPage {

	private NewFeatureTableViewer tableViewer;
	
	protected SubWellFeaturePage() {
		super("SubWell Features");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		tableViewer = new NewFeatureTableViewer(container);
		GridDataFactory.fillDefaults().grab(true,false).hint(SWT.DEFAULT, 350).applyTo(tableViewer.getControl());
		
		setTitle("SubWell Features");
    	setDescription("Select the SubWell Features that should be included in the Protocol.");
    	setControl(container);
    	setPageComplete(true);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		tableViewer.setInput(((WizardState)state).subwellFeatures);
	}
}
