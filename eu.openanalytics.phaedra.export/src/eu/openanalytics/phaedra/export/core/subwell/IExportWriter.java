package eu.openanalytics.phaedra.export.core.subwell;

public interface IExportWriter {

	public final static int MODE_PAGE_PER_WELL = 1;
	public final static int MODE_FILE_PER_WELL = 2;
	public final static int MODE_ONE_PAGE = 3;
	
	public final static int WELLTYPE_ALL = 0;
	public final static int WELLTYPE_SAMPLE = 1;
	public final static int WELLTYPE_CONTROL = 2;
	
}