package eu.openanalytics.phaedra.model.curve;

import eu.openanalytics.phaedra.model.curve.vo.Curve;

public interface ICurveFitModel {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".fitModel";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public String getId();
	public String getDescription();
	
	public CurveParameter.Definition[] getInputParameters();
	public CurveParameter.Definition[] getOutputParameters();
	
	public CurveFitErrorCode[] getErrorCodes();
	
	public void validateInput(CurveFitInput input) throws CurveFitException;
	public void fit(CurveFitInput input, Curve output) throws CurveFitException;
	
}
