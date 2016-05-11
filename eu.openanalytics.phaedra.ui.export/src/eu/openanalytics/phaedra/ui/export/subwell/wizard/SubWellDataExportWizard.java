package eu.openanalytics.phaedra.ui.export.subwell.wizard;

import java.util.List;

import org.eclipse.jface.wizard.Wizard;

import eu.openanalytics.phaedra.export.core.subwell.ExportSettings;
import eu.openanalytics.phaedra.export.core.subwell.SubWellDataWriterFactory;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.export.subwell.wizard.pages.FeatureSelectionPage;
import eu.openanalytics.phaedra.ui.export.subwell.wizard.pages.FileLocationPage;
import eu.openanalytics.phaedra.ui.export.subwell.wizard.pages.SettingsSelectionPage;

public class SubWellDataExportWizard extends Wizard {

	private ProtocolClass pClass;
	private List<Well> wells;

	private ExportSettings settings;

	public SubWellDataExportWizard(List<Well> wells) {
		setWindowTitle("Export Subwell Data");

		this.pClass = (ProtocolClass) wells.get(0).getAdapter(ProtocolClass.class);
		this.wells = wells;

		this.settings = new ExportSettings();
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(new FeatureSelectionPage(pClass, settings));
		addPage(new SettingsSelectionPage(settings));
		addPage(new FileLocationPage(settings));
	}

	@Override
	public boolean performFinish() {
		// Create the Excel File
		SubWellDataWriterFactory.write(wells, settings);

		return true;
	}

}