package eu.openanalytics.phaedra.ui.export.wizard.plate;

import java.util.Collections;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.export.core.ExportPlateTableSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractExperimentsPage;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class ExperimentsPage extends AbstractExperimentsPage {
	
	private final ExportPlateTableSettings settings;
	
	public ExperimentsPage(ExportPlateTableSettings settings, int stepNum, int stepTotal) {
		super(settings, stepNum, stepTotal);
		
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
	protected void pageAboutToShow(boolean firstTime) {
		super.pageAboutToShow(firstTime);
		
		if (!firstTime) return;
		
		if (settings.getFeatures().isEmpty()) {
			Feature currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
			if (currentFeature != null) {
				setFeatureSelection(Collections.singleton(currentFeature));
			}
		}
	}
	
	@Override
	protected boolean validateSettings() {
		return super.validateSettings()
				&& (!settings.getFeatures().isEmpty());
	}
	
}
