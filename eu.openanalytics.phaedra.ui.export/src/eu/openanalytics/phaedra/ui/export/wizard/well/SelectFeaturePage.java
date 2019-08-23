package eu.openanalytics.phaedra.ui.export.wizard.well;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractExperimentsPage;

public class SelectFeaturePage extends AbstractExperimentsPage {
	
	private final ExportSettings settings;
	
	
	public SelectFeaturePage(ExportSettings settings) {
		super("Select Features", settings);
		setDescription("Step 1/4: Select the features to export.");
		
		this.settings = settings;
	}
	
	
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		setControl(container);
		
		addExperimentsInfo(container);
		
		addFeatureSelection(container, settings.getFeatures());
		
		setPageComplete(false);
	}
	
	
	@Override
	protected boolean validateSettings() {
		return super.validateSettings()
				&& (!settings.getFeatures().isEmpty());
	}
	
}
