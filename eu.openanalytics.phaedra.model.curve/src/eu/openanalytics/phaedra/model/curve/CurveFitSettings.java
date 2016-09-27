package eu.openanalytics.phaedra.model.curve;

import java.util.Arrays;

public class CurveFitSettings {

	public static final String MODEL = "MODEL";
	public static final String GROUP_BY_1 = "GROUP_BY_1";
	public static final String GROUP_BY_2 = "GROUP_BY_2";
	public static final String GROUP_BY_3 = "GROUP_BY_3";
	
	private String modelId;
	private String[] groupingFeatures;
	private CurveParameter.Value[] extraParameters;

	public String getModelId() {
		return modelId;
	}
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	
	public String[] getGroupingFeatures() {
		return groupingFeatures;
	}
	public void setGroupingFeatures(String[] groupingFeatures) {
		this.groupingFeatures = groupingFeatures;
	}
	
	public CurveParameter.Value[] getExtraParameters() {
		return extraParameters;
	}
	public void setExtraParameters(CurveParameter.Value[] extraParameters) {
		this.extraParameters = extraParameters;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(extraParameters);
		result = prime * result + Arrays.hashCode(groupingFeatures);
		result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CurveFitSettings other = (CurveFitSettings) obj;
		if (!Arrays.equals(extraParameters, other.extraParameters))
			return false;
		if (!Arrays.equals(groupingFeatures, other.groupingFeatures))
			return false;
		if (modelId == null) {
			if (other.modelId != null)
				return false;
		} else if (!modelId.equals(other.modelId))
			return false;
		return true;
	}
}
