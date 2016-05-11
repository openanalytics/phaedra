package eu.openanalytics.phaedra.ui.columbus.importwizard;

public class Meas {
	
	public String name;
	public String barcode;
	public String source;
	public boolean isIncluded;
	
	public MeasAnalysis[] availableAnalyses;
	public MeasAnalysis selectedAnalysis;
	
	public static class MeasAnalysis {
		public String name;
		public String source;
	}
}