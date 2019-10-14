package eu.openanalytics.phaedra.export.core.subwell;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public interface IExportWriter {

	public final static int MODE_PAGE_PER_WELL = 1;
	public final static int MODE_FILE_PER_WELL = 2;
	public final static int MODE_ONE_PAGE = 3;
	
	public final static int WELLTYPE_ALL = 0;
	public final static int WELLTYPE_SAMPLE = 1;
	public final static int WELLTYPE_CONTROL = 2;
	
	
	void initialize(final IExportExperimentsSettings settings, final DataFormatter dataFormatter);
	
	/**
	 * Adds an entry to the export information.
	 */
	void addExportInfo(ExportInfo info);
	
	void write(final List<Well> wells, final ExportSettings settings);
	
}
