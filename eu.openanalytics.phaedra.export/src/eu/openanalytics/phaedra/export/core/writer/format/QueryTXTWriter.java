package eu.openanalytics.phaedra.export.core.writer.format;


public class QueryTXTWriter extends QueryCSVWriter {

	public QueryTXTWriter() {
		super('\t', au.com.bytecode.opencsv.CSVWriter.NO_QUOTE_CHARACTER, au.com.bytecode.opencsv.CSVWriter.NO_ESCAPE_CHARACTER);
	}

	@Override
	protected boolean needQuoting() {
		return false;
	}
}
