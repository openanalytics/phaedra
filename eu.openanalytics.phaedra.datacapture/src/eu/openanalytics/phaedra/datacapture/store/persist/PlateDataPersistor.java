package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.DefaultDataCaptureStore;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.source.IPlateDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateDataPersistor extends BaseDataPersistor {

	// Note: do not modify: these names are used in DC scripts that capture layout data.
	private static final String PLATE_LAYOUT_PREFIX = "plate.layout";
	private static final String PROP_WELL_TYPES = "WellTypes";
	private static final String PROP_WELL_CONCS = "WellConcentrations";
	private static final String PROP_COMP_TYPES = "CompoundTypes";
	private static final String PROP_COMP_NRS = "CompoundNumbers";
	private static final String PROP_ROWS = "rows";
	private static final String PROP_COLUMNS = "columns";
	
	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		// Save plate properties, if any.
		Map<String,String> plateProperties = new HashMap<>();
		for (String property: getNames(store, DefaultDataCaptureStore.PLATE_PROPERTY_PREFIX)) {
			String value = getPropertyValue(property, store);
			if (value != null) plateProperties.put(property, value);
		}
		if (!plateProperties.isEmpty()) PlateService.getInstance().setPlateProperties(plate, plateProperties);

		// Save plate layout, if any.
		PlateTemplate layout = getLayout(store);
		if (layout != null) {
			PlateLinkSettings settings = new PlateLinkSettings();
			settings.setPlate(plate);
			settings.getSettings().put("template", layout);
			try {
				IPlateDefinitionSource source = PlateDefinitionService.getInstance().getSource("Layout Template");
				PlateDefinitionService.getInstance().linkSource(source, settings);
			} catch (PlateLinkException e) {
				throw new DataCaptureException("Failed to apply plate layout: " + e.getMessage(), e);
			}
		}
	}

	private String getPropertyValue(String property, IFileStore store) throws IOException {
		Object value = store.readStringValue(DefaultDataCaptureStore.PLATE_PROPERTY_PREFIX + property);
		return (value == null) ? null : String.valueOf(value);
	}
	
	public PlateTemplate getLayout(IFileStore store) throws IOException {
		String rows = null;
		String columns = null;
		try {
			rows = store.readStringValue(PLATE_LAYOUT_PREFIX + PROP_ROWS);
			columns = store.readStringValue(PLATE_LAYOUT_PREFIX + PROP_COLUMNS);
		} catch (IOException e) {}
		if (rows == null || columns == null) return null;
		
		PlateTemplate template = new PlateTemplate();
		template.fillBlank((int) Double.parseDouble(rows), (int) Double.parseDouble(columns));

		applyWellTypes(template, store.readStringArray(PLATE_LAYOUT_PREFIX + PROP_WELL_TYPES));
		applyConcentrations(template, store.readStringArray(PLATE_LAYOUT_PREFIX + PROP_WELL_CONCS));
		applyCompTypes(template, store.readStringArray(PLATE_LAYOUT_PREFIX + PROP_COMP_TYPES));
		applyCompNrs(template, store.readStringArray(PLATE_LAYOUT_PREFIX + PROP_COMP_NRS));

		return template;
	}
	
	private void applyWellTypes(PlateTemplate template, String[] values) {
		if (values == null || values.length == 0) return;
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setWellType(values[nr]);
		}
	}

	private void applyCompTypes(PlateTemplate template, String[] values) {
		if (values == null || values.length == 0) return;
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setCompoundType(values[nr]);
		}
	}

	private void applyCompNrs(PlateTemplate template, String[] values) {
		if (values == null || values.length == 0) return;
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setCompoundNumber(values[nr]);
		}
	}

	private void applyConcentrations(PlateTemplate template, String[] values) {
		if (values == null || values.length == 0) return;
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setConcentration(values[nr]);
		}
	}
}
