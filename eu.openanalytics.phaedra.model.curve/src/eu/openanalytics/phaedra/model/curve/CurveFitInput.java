package eu.openanalytics.phaedra.model.curve;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class CurveFitInput {
	
	private List<Well> wells;
	
	private double[] concs;
	private double[] values;
	private boolean[] valid;
	
	private CurveFitSettings settings;

	public List<Well> getWells() {
		return wells;
	}
	
	public void setWells(List<Well> wells) {
		this.wells = wells;
	}
	
	public double[] getConcs() {
		return concs;
	}

	public void setConcs(double[] concs) {
		this.concs = concs;
	}

	public double[] getValues() {
		return values;
	}

	public void setValues(double[] values) {
		this.values = values;
	}

	public boolean[] getValid() {
		return valid;
	}

	public void setValid(boolean[] valid) {
		this.valid = valid;
	}

	public CurveFitSettings getSettings() {
		return settings;
	}
	
	public void setSettings(CurveFitSettings settings) {
		this.settings = settings;
	}
}
