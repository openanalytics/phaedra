package eu.openanalytics.phaedra.model.plate.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;
import org.eclipse.persistence.config.CacheIsolationType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

@Entity
@Table(name="hca_feature_value", schema="phaedra")
@Cache(isolation = CacheIsolationType.ISOLATED)
public class FeatureValue extends PlatformObject implements Serializable {

	private static final long serialVersionUID = 4582783400996028819L;

	@Id
	@Column(name="well_id", insertable=false, updatable=false)
	private long wellId;

	@Id
	@Column(name="feature_id", insertable=false, updatable=false)
	private long featureId;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="well_id")
	private Well well;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="feature_id")
	private Feature feature;

	@Column(name="raw_numeric_value")
	private double rawNumericValue;

	@IgnoreSizeOf
	@Column(name="raw_string_value")
	private String rawStringValue;

	@Column(name="normalized_value")
	private double normalizedValue;

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */

	public long getWellId() {
		return wellId;
	}

	public void setWellId(long wellId) {
		this.wellId = wellId;
	}

	public long getFeatureId() {
		return featureId;
	}

	public void setFeatureId(long featureId) {
		this.featureId = featureId;
	}

	public Well getWell() {
		return well;
	}

	public void setWell(Well well) {
		this.well = well;
	}

	public Feature getFeature() {
		return feature;
	}

	public void setFeature(Feature feature) {
		this.feature = feature;
	}

	public double getRawNumericValue() {
		return rawNumericValue;
	}

	public void setRawNumericValue(double rawNumericValue) {
		this.rawNumericValue = rawNumericValue;
	}

	public String getRawStringValue() {
		return rawStringValue;
	}

	public void setRawStringValue(String rawStringValue) {
		this.rawStringValue = rawStringValue;
	}

	public double getNormalizedValue() {
		return normalizedValue;
	}

	public void setNormalizedValue(double normalizedValue) {
		this.normalizedValue = normalizedValue;
	}
}
