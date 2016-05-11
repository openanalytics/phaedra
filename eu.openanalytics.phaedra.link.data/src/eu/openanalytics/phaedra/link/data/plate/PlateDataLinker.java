package eu.openanalytics.phaedra.link.data.plate;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkException;
import eu.openanalytics.phaedra.link.data.IDataLinkerComponent;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.source.IPlateDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
 * This component links the following items to the target plate:
 * <ul>
 * <li>Properties: key-value pairs found at the root of the HDF5 file.</li>
 * <li>Layout: plate dimensions and layout info found in the layout group (see HDF5File.getPlateLayoutPath()).</li>
 * </ul>
 */
public class PlateDataLinker implements IDataLinkerComponent {

	private Plate plate;
	
	private Map<String,String> propertiesToSet;
	private PlateTemplate layoutToApply;
	
	private final static String PLATE_PROPS_PATH = "/";
	
	@Override
	public void prepareLink(PlateReading reading, HDF5File dataFile, Plate destination, IProgressMonitor monitor) throws DataLinkException {

		plate = destination;
		
		propertiesToSet = new HashMap<String, String>();
		if (dataFile.exists(PLATE_PROPS_PATH)) {
			String[] props = dataFile.getAttributes(PLATE_PROPS_PATH);
			for (String prop: props) {
				Object value = dataFile.getAttribute(PLATE_PROPS_PATH, prop);
				if (value != null) propertiesToSet.put(prop, value.toString());
			}
		}
		
		if (dataFile.exists(HDF5File.getPlateLayoutPath())) {
			PlateTemplateGenerator generator = new PlateTemplateGenerator(dataFile);
			layoutToApply = generator.generate();
		}
	}

	@Override
	public void executeLink() throws DataLinkException {
		
		if (!propertiesToSet.isEmpty()) {
			// Make sure plate exists before setting its properties.
			boolean newPlate = (plate.getId() == 0);
			if (newPlate) PlateService.getInstance().updatePlate(plate);
			
			PlateService.getInstance().setPlateProperties(plate, propertiesToSet);
		}
		
		if (layoutToApply != null) {
			PlateLinkSettings settings = new PlateLinkSettings();
			settings.setPlate(plate);
			settings.getSettings().put("template", layoutToApply);
			try {
				IPlateDefinitionSource source = PlateDefinitionService.getInstance().getSource("Layout Template");
				PlateDefinitionService.getInstance().linkSource(source, settings);
			} catch (PlateLinkException e) {
				throw new DataLinkException("Failed to apply plate layout: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void rollback() {
		// Nothing to rollback.
	}
}
