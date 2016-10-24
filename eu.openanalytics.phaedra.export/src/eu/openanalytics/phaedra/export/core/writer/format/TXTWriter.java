package eu.openanalytics.phaedra.export.core.writer.format;


public class TXTWriter extends CSVWriter {

	public TXTWriter() {
		super('\t', au.com.bytecode.opencsv.CSVWriter.NO_QUOTE_CHARACTER, au.com.bytecode.opencsv.CSVWriter.NO_ESCAPE_CHARACTER);
	}

	@Override
	protected boolean needQuoting() {
		return false;
	}
}
