package eu.openanalytics.phaedra.model.curve.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public abstract class Curve extends PlatformObject implements Serializable {

	private static final long serialVersionUID = -8890045409137210115L;

	private long id;
	
	private transient Feature feature;
	private transient List<Compound> compounds;

	private CurveSettings settings;

	private int fitError;
	private String fitMessage;
	private String fitVersion;
	private Date fitDate;

	private byte[] plot;

	private double eMax;
	private double eMaxConc;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
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
	public CurveSettings getSettings() {
		return settings;
	}
	public void setSettings(CurveSettings settings) {
		this.settings = settings;
	}
	public int getFitError() {
		return fitError;
	}
	public void setFitError(int fitError) {
		this.fitError = fitError;
	}
	public String getFitMessage() {
		return fitMessage;
	}
	public void setFitMessage(String fitMessage) {
		this.fitMessage = fitMessage;
	}
	public String getFitVersion() {
		return fitVersion;
	}
	public void setFitVersion(String fitVersion) {
		this.fitVersion = fitVersion;
	}
	public Date getFitDate() {
		return fitDate;
	}
	public void setFitDate(Date fitDate) {
		this.fitDate = fitDate;
	}
	public byte[] getPlot() {
		return plot;
	}
	public void setPlot(byte[] plot) {
		this.plot = plot;
	}
	public double geteMax() {
		return eMax;
	}
	public void seteMax(double eMax) {
		this.eMax = eMax;
	}
	public double geteMaxConc() {
		return eMaxConc;
	}
	public void seteMaxConc(double eMaxConc) {
		this.eMaxConc = eMaxConc;
	}
	
	@Override
	public String toString() {
		String kind = "";
		if (getSettings() != null) kind = getSettings().getKind();
		String cmp = "";
		if (compounds != null) {
			if (compounds.isEmpty()) cmp = "<no compound>";
			cmp = compounds.get(0).toString();
			if (compounds.size() > 1) cmp += " @ " + compounds.size() + " plates";
		}
		return kind + "Curve (" + id + "): " + feature + " @ " + cmp;
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
