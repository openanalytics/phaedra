package eu.openanalytics.phaedra.datacapture.montage;

public class MontageConfig {

	public String layoutSource;
	public String layout;
	public int padding;
	public int startingFieldNr;
	
	public String subwellDataPath;
	public String subwellDataPattern;
	public String subwellDataPatternIdGroups;
	public String subwellDataPatternFieldGroup;
	public String subwellDataOutput;
	public String subwellDataParserId;
	public String[] subwellDataXFeatures;
	public String[] subwellDataYFeatures;

	public ImageComponent[] imageComponents;
	
	public static class ImageComponent {
		public String path;
		public String pattern;
		public String patternIdGroups;
		public String patternFieldGroup;
		public String frame;
		public String output;
	}
}
