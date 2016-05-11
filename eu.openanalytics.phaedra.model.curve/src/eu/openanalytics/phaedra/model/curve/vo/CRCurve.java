package eu.openanalytics.phaedra.model.curve.vo;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;
import org.eclipse.persistence.config.CacheIsolationType;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This JPA entity should be used only in the search framework!
 * For all other purposes, use {@link Curve}
 */
@Entity
@Table(name="hca_curves", schema="phaedra")
@Cache(isolation = CacheIsolationType.ISOLATED)
public class CRCurve extends PlatformObject implements IValueObject {

	@Id
	@Column(name="curve_id")
	private long curveId;

	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="feature_id")
	private Feature feature;
	
	@ManyToMany
    @JoinTable(name="hca_curve_compound", schema="phaedra",
    	joinColumns=@JoinColumn(name="curve_id"), inverseJoinColumns=@JoinColumn(name="platecompound_id"))
	private List<Compound> compounds;
	
	@Column(name="KIND")
	private String kind;

	@Column(name="METHOD")
	private String method;

	@Column(name="MODEL")
	private String model;

	@Column(name="TYPE")
	private String type;

	@Column(name="EMAX")
	private double eMax;

	@Column(name="EMAX_CONC")
	private double eMaxConc;

	@Column(name="FIT_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fitDate;
	
	@Column(name="FIT_ERROR")
	private double fitError;

	@Column(name="FIT_VERSION")
	private String fitVersion;

	@Column(name="PIC50")
	private double pic50;

	@Column(name="PIC50_CENSOR")
	private String pic50Censor;

	@Column(name="PIC50_STDERR")
	private double pic50StdErr;

	@Column(name="R2")
	private double r2;

	@Column(name="HILL")
	private double hill;

	@Column(name="PLAC")
	private double plac;

	@Column(name="PLAC_CENSOR")
	private String placCensor;

	@Column(name="PLAC_THRESHOLD")
	private double placThreshold;

	@Column(name="NIC")
	private double nic;

	@Column(name="NAC")
	private double nac;

	
	/*
	 * Getters and setters
	 * *******************
	 */

	public Feature getFeature() {
		return feature;
	}

	public void setFeature(Feature feature) {
		this.feature = feature;
	}

	public long getCurveId() {
		return curveId;
	}
	
	public void setCurveId(long curveId) {
		this.curveId = curveId;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public Date getFitDate() {
		return fitDate;
	}
	
	public void setFitDate(Date fitDate) {
		this.fitDate = fitDate;
	}
	
	public double getFitError() {
		return fitError;
	}

	public void setFitError(double fitError) {
		this.fitError = fitError;
	}

	public String getFitVersion() {
		return fitVersion;
	}

	public void setFitVersion(String fitVersion) {
		this.fitVersion = fitVersion;
	}

	public double getNic() {
		return nic;
	}

	public void setNic(double nic) {
		this.nic = nic;
	}

	public double getNac() {
		return nac;
	}

	public void setNac(double nac) {
		this.nac = nac;
	}

	public double getPic50() {
		return pic50;
	}

	public void setPic50(double pic50) {
		this.pic50 = pic50;
	}

	public String getPic50Censor() {
		return pic50Censor;
	}

	public void setPic50Censor(String pic50Censor) {
		this.pic50Censor = pic50Censor;
	}

	public double getPic50StdErr() {
		return pic50StdErr;
	}

	public void setPic50StdErr(double pic50StdErr) {
		this.pic50StdErr = pic50StdErr;
	}

	public double getR2() {
		return r2;
	}

	public void setR2(double r2) {
		this.r2 = r2;
	}

	public double getHill() {
		return hill;
	}

	public void setHill(double hill) {
		this.hill = hill;
	}

	public double getPlac() {
		return plac;
	}

	public void setPlac(double plac) {
		this.plac = plac;
	}

	public String getPlacCensor() {
		return placCensor;
	}

	public void setPlacCensor(String placCensor) {
		this.placCensor = placCensor;
	}

	public double getPlacThreshold() {
		return placThreshold;
	}

	public void setPlacThreshold(double placThreshold) {
		this.placThreshold = placThreshold;
	}

	/*
	 * Convenience methods
	 * *******************
	 */

	@Override
	public long getId() {
		return curveId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (curveId ^ (curveId >>> 32));
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
		CRCurve other = (CRCurve) obj;
		if (curveId != other.curveId)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return null;
	}

}
