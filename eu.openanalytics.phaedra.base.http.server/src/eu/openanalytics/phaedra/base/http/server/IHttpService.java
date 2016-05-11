package eu.openanalytics.phaedra.base.http.server;

public interface IHttpService {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".httpService";
	public final static String ATTR_ID = "id";
	public final static String ATTR_CLASS = "class";
	
	public void startup();
	public void shutdown();
}
