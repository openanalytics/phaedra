package eu.openanalytics.phaedra.model.curve;

public interface ICurveFitModelFactory {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".fitModelFactory";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";

	public String[] getSupportedModelIds();
	public ICurveFitModel createModel(String id);

}
