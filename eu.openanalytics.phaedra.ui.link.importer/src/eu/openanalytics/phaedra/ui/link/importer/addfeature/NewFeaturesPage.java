package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.data.addfeatures.AddFeaturesTask;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.link.importer.addfeature.AddFeaturesWizard.AddFeaturesWizardState;


public class NewFeaturesPage extends BaseStatefulWizardPage {

	private NewFeatureTableViewer newFeaturesTableViewer;
	private String featureType;
	
	public NewFeaturesPage() {
		super("Select features to be added to the Protocol Class");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);
		
		newFeaturesTableViewer = new NewFeatureTableViewer(container);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(newFeaturesTableViewer.getControl());

	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		AddFeaturesTask task = ((AddFeaturesWizardState)state).task;
		if (task.featureDefinitions != null) newFeaturesTableViewer.setInput(task.featureDefinitions);
		featureType = (task.featureClass == Feature.class) ? "well" : "subwell";
		
		setTitle("There are " + featureType + " features in the import data that are not present in the protocol class");
		setPageComplete(true);
	}

	@Override
	public void collectState(IWizardState state) {
		AddFeaturesTask task = ((AddFeaturesWizardState)state).task;
		task.featureDefinitions = newFeaturesTableViewer.getNewFeatureDefinitions();
	}
}
