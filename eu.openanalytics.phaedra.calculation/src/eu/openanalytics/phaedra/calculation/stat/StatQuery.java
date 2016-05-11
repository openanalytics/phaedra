package eu.openanalytics.phaedra.calculation.stat;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class StatQuery {

	private IValueObject object;
	private String stat;
	
	private IFeature feature;
	
	private String wellType;
	private String normalization;

	private boolean includeRejected;
	
	public StatQuery() {
		// Default constructor
	}
	
	public StatQuery(String stat, IValueObject object, IFeature feature, String wellType, String normalization) {
		this(stat, object, feature, wellType, normalization, false);
	}
	
	public StatQuery(String stat, IValueObject object, IFeature feature, String wellType, String normalization, boolean includeRejected) {
		this.stat = stat;
		this.object = object;
		this.wellType = wellType;
		this.feature = feature;
		this.normalization = normalization;
		this.includeRejected = includeRejected;
	}
	
	public String getStat() {
		return stat;
	}
	public void setStat(String stat) {
		this.stat = stat;
	}
	public IValueObject getObject() {
		return object;
	}
	public void setObject(IValueObject object) {
		this.object = object;
	}
	public String getWellType() {
		return wellType;
	}
	public void setWellType(String wellType) {
		this.wellType = wellType;
	}
	public IFeature getFeature() {
		return feature;
	}
	public void setFeature(IFeature feature) {
		this.feature = feature;
	}
	public String getNormalization() {
		return normalization;
	}
	public void setNormalization(String normalization) {
		this.normalization = normalization;
	}
	public boolean isIncludeRejected() {
		return includeRejected;
	}
	public void setIncludeRejected(boolean includeRejected) {
		this.includeRejected = includeRejected;
	}
}
