package eu.openanalytics.phaedra.ui.link.importer.wizard.page;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.link.importer.util.IPlateMapListener;
import eu.openanalytics.phaedra.ui.link.importer.util.PlateMapper;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;


public class MapPlatesPage extends BaseStatefulWizardPage {

	private PlateMapper plateMapper;
	
	public MapPlatesPage() {
		super("Map Plates");
		setTitle("Map Plates");
		setDescription("For each reading, specify the destination plate by dragging"
				+ " the reading from the left table onto a plate in the right table.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);
		
		plateMapper = new PlateMapper(container, SWT.NONE);
		plateMapper.addListener(new IPlateMapListener() {
			@Override
			public void plateMapped(PlateReading source, Plate plate) {
				setPageComplete(true);
			}
		});
		//TODO Disable finish if all mappings are removed.
		GridDataFactory.fillDefaults().indent(15,0).grab(true,true).applyTo(plateMapper);
		
		setPageComplete(false);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		ImportWizardState wizardState = ((ImportWizardState)state);

		// Do a quick scan to locate plate files/folders.
		PlateReading[] readings = ImportUtils.locatePlates(
				wizardState.task.sourcePath,
				wizardState.task.getCaptureConfigId());
		plateMapper.setReadings(readings);
		
		// Fetch the existing plates from the experiment.
		List<Plate> plates = PlateService.getInstance().getPlates(wizardState.task.targetExperiment);
		plateMapper.setPlates(plates.toArray(new Plate[plates.size()]));
		
		plateMapper.mapAll();
		
		if (readings != null && readings.length > 0 && plateMapper.getMapping().isEmpty()) setPageComplete(false);
		else setPageComplete(true);
	}
	
	@Override
	public void collectState(IWizardState state) {
		ImportWizardState wizardState = ((ImportWizardState)state);
		wizardState.task.plateMapping = plateMapper.getMapping();
	}
}
