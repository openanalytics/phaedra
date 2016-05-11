package eu.openanalytics.phaedra.link.data.plate;

import java.io.IOException;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;

/**
 * Utility class to generate a PlateTemplate out of a HDF5 file containing
 * layout information.
 * 
 * @see HDF5File#getPlateLayoutPath()
 */
public class PlateTemplateGenerator {

	private HDF5File dataFile;
	
	public final static String WELL_TYPES_PATH = "/WellTypes";
	public final static String COMP_TYPES_PATH = "/CompoundTypes";
	public final static String COMP_NRS_PATH = "/CompoundNumbers";
	public final static String CONCS_PATH = "/Concentrations";
	
	public final static String ROWS_ATTR = "rows";
	public final static String COLS_ATTR = "columns";
	
	public PlateTemplateGenerator(HDF5File dataFile) {
		this.dataFile = dataFile;
	}
	
	public PlateTemplate generate() {
		PlateTemplate template = new PlateTemplate();
		
		try {
			String path = HDF5File.getPlateLayoutPath();
			
			// Look up and apply plate dimensions.
			Object rowsValue = dataFile.getAttribute(path, ROWS_ATTR);
			Object colsValue = dataFile.getAttribute(path, COLS_ATTR);
			if (rowsValue == null || colsValue == null) {
				throw new RuntimeException("Plate dimensions not specified in " + HDF5File.getPlateLayoutPath() + "/@" + ROWS_ATTR + " and @" + COLS_ATTR);
			}
			int rows = (int)(Double.parseDouble(rowsValue.toString()));
			int cols = (int)(Double.parseDouble(colsValue.toString()));
			template.fillBlank(rows, cols);
			
			if (dataFile.exists(path + WELL_TYPES_PATH)) {
				String[] values = dataFile.getStringData1D(path + WELL_TYPES_PATH);
				applyWellTypes(template, values);
			}
			
			if (dataFile.exists(path + COMP_TYPES_PATH)) {
				String[] values = dataFile.getStringData1D(path + COMP_TYPES_PATH);
				applyCompTypes(template, values);
			}
			
			if (dataFile.exists(path + COMP_NRS_PATH)) {
				String[] values = dataFile.getStringData1D(path + COMP_NRS_PATH);
				applyCompNrs(template, values);
			}
			
			if (dataFile.exists(path + CONCS_PATH)) {
				String[] values = dataFile.getStringData1D(path + CONCS_PATH);
				applyConcentrations(template, values);
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Failed to create plate template", e);
		}
		
		return template;
	}
	
	private void applyWellTypes(PlateTemplate template, String[] values) {
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setWellType(values[nr]);
		}
	}

	private void applyCompTypes(PlateTemplate template, String[] values) {
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setCompoundType(values[nr]);
		}
	}

	private void applyCompNrs(PlateTemplate template, String[] values) {
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setCompoundNumber(values[nr]);
		}
	}

	private void applyConcentrations(PlateTemplate template, String[] values) {
		int wellCount = template.getWells().size();
		for (int nr=0; nr<wellCount; nr++) {
			WellTemplate well = template.getWells().get(nr+1);
			well.setConcentration(values[nr]);
		}
	}
}
