package eu.openanalytics.phaedra.export.core.writer;

import java.io.IOException;

import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;

public interface IExportWriter {

	/**
	 * Configure this export writer to use the provided value converter.
	 * If not set, the writer will not perform value conversion.
	 * 
	 * @param valueConverter The value converter to use.
	 */
	public void setValueConverter(IValueConverter valueConverter);
	
	/**
	 * Initialize the writer, possibly opening and/or creating files.
	 * 
	 * @param settings The settings to use for exporting.
	 * @throws IOException If the initialization fails because of an I/O exception.
	 */
	public void initialize(IExportExperimentsSettings settings) throws IOException;
	
	/**
	 * Adds an entry to the export information.
	 */
	void addExportInfo(ExportInfo info);
	
	/**
	 * Write the base data (the data that is not feature-specific) to the destination.
	 * 
	 * @param result The base data.
	 * @throws IOException If the export fails because of an I/O exception.
	 */
	public void writeBaseData(QueryResult result) throws IOException;
	
	/**
	 * Write the export data for one Feature to the destination.
	 * 
	 * @param result The export data for one Feature.
	 * @throws IOException If the export fails because of an I/O exception.
	 */
	public void writeFeature(QueryResult result) throws IOException;
	
	/**
	 * Complete the export operation. Perform any actions that are needed e.g. to close the file(s).
	 */
	public void finish() throws IOException;
	
	/**
	 * Roll back any actions taken so far. This method is called if the export is interrupted
	 * or fails for any reason.
	 */
	public void rollback();
}
