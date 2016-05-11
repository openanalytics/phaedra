package eu.openanalytics.phaedra.base.ui.trafficlight;

public interface IStatusChecker {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".statusChecker";
	public final static String ATTR_CLASS = "class";
	
	public String getName();
	
	public String getDescription();
	
	public char getIconLetter();
	
	public long getPollInterval();
	
	public TrafficStatus poll();
	
	public TrafficStatus test();
}
