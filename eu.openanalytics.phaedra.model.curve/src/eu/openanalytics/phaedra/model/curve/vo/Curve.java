package eu.openanalytics.phaedra.model.curve.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class Curve extends PlatformObject {

	private long id;
	
	private Feature feature;
	private List<Compound> compounds;
	
	private String modelId;
	
	private String[] groupingValues;
	
	private Date fitDate;
	private String fitVersion;
	private int errorCode;
	private CurveParameter.Value[] outputParameters;
	
	private byte[] plot;
	
	public Curve() {
		compounds = new ArrayList<>();
	}
	
	public Feature getFeature() {
		return feature;
	}
	public void setFeature(Feature feature) {
		this.feature = feature;
	}
	public List<Compound> getCompounds() {
		return compounds;
	}
	public void setCompounds(List<Compound> compounds) {
		this.compounds = compounds;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getModelId() {
		return modelId;
	}
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	public String[] getGroupingValues() {
		return groupingValues;
	}
	public void setGroupingValues(String[] groupingValues) {
		this.groupingValues = groupingValues;
	}
	public Date getFitDate() {
		return fitDate;
	}
	public void setFitDate(Date fitDate) {
		this.fitDate = fitDate;
	}
	public String getFitVersion() {
		return fitVersion;
	}
	public void setFitVersion(String fitVersion) {
		this.fitVersion = fitVersion;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public CurveParameter.Value[] getOutputParameters() {
		return outputParameters;
	}
	public void setOutputParameters(CurveParameter.Value[] outputParameters) {
		this.outputParameters = outputParameters;
	}
	public byte[] getPlot() {
		return plot;
	}
	public void setPlot(byte[] plot) {
		this.plot = plot;
	}
	
	@Override
	public String toString() {
		String cmp = (compounds.size() == 1) ? compounds.get(0).toString() : (compounds.size() + " compounds");
		return String.format("Curve [%s] [%s] [model: %s] [id: %s]", feature, cmp, modelId, id);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		Curve other = (Curve) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
