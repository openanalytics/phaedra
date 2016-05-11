package eu.openanalytics.phaedra.ui.export.subwell.wizard.pages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.export.core.subwell.ExportSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.protocol.util.FeatureSelectionTable;

public class FeatureSelectionPage extends WizardPage {

	public static final String PAGE_NAME = "Feature Selection";

	private ProtocolClass pClass;
	private ExportSettings settings;

	private FeatureSelectionTable<SubWellFeature> featureSelectionTable;

	public FeatureSelectionPage(ProtocolClass pClass, ExportSettings settings) {
		super(PAGE_NAME);
		setTitle(PAGE_NAME);
		setDescription("Select the Features which you want to export");

		this.pClass = pClass;
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		this.featureSelectionTable = new FeatureSelectionTable<SubWellFeature>(parent, SWT.NONE, pClass, SubWellFeature.class, settings.getSelectedFeatures());
		this.featureSelectionTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(featureSelectionTable);
		GridLayoutFactory.fillDefaults().applyTo(featureSelectionTable);

		checkPageComplete();
		setControl(featureSelectionTable);
	}
	
	private void checkPageComplete() {
		setPageComplete(!settings.getSelectedFeatures().isEmpty());
	}

}