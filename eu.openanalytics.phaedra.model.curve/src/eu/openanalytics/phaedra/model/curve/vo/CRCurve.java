package eu.openanalytics.phaedra.model.curve.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;
import org.eclipse.persistence.config.CacheIsolationType;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.IListAdaptable;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This JPA entity should be used only in the search framework!
 * For all other purposes, use {@link Curve}
 */
@Entity
@Table(name="hca_curve", schema="phaedra")
@Cache(isolation = CacheIsolationType.ISOLATED)
public class CRCurve extends PlatformObject implements IValueObject, IListAdaptable {

	@Id
	@Column(name="curve_id")
	private long id;

	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="feature_id")
	private Feature feature;
	
	@ManyToMany
    @JoinTable(name="hca_curve_compound", schema="phaedra",
    	joinColumns=@JoinColumn(name="curve_id"), inverseJoinColumns=@JoinColumn(name="platecompound_id"))
	private List<Compound> compounds;

	@Column(name="model_id")
	private String model;
	
	@Column(name="group_by_1")
	private String groupBy1;
	@Column(name="group_by_2")
	private String groupBy2;
	@Column(name="group_by_3")
	private String groupBy3;

	@Column(name="fit_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fitDate;
	
	@Column(name="fit_version")
	private String fitVersion;
	
	@Column(name="error_code")
	private int errorCode;
		
	@ElementCollection(targetClass=PropertyValue.class)
	@MapKeyColumn(name="property_name")
	@Column(name="numeric_value")
	@CollectionTable(name="hca_curve_property", schema="phaedra", joinColumns=@JoinColumn(name="curve_id"))
	private Map<String,PropertyValue> properties;
	
	/*
	 * Getters and setters
	 * *******************
	 */

	@Override
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getGroupBy1() {
		return groupBy1;
	}

	public void setGroupBy1(String groupBy1) {
		this.groupBy1 = groupBy1;
	}

	public String getGroupBy2() {
		return groupBy2;
	}

	public void setGroupBy2(String groupBy2) {
		this.groupBy2 = groupBy2;
	}

	public String getGroupBy3() {
		return groupBy3;
	}

	public void setGroupBy3(String groupBy3) {
		this.groupBy3 = groupBy3;
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

	public Map<String, PropertyValue> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, PropertyValue> properties) {
		this.properties = properties;
	}

	/*
	 * Convenience methods
	 * *******************
	 */

	@Override
	public <T> List<T> getAdapterList(Class<T> type) {
		if (type == Compound.class) {
			return (List<T>) getCompounds();
		}
		if (type == Plate.class) {
			List<Compound> compounds = getCompounds();
			List<Plate> plates = new ArrayList<Plate>(compounds.size());
			for (Compound compound : compounds) {
				plates.add(compound.getPlate());
			}
			return (List<T>) plates;
		}
		return null;
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
		CRCurve other = (CRCurve) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return null;
	}

	@Embeddable
	public static class PropertyValue {
		
		@Column(name="string_value")
		private String stringValue;
		
		@Column(name="numeric_value")
		private double numericValue;
		
		public String getStringValue() {
			return stringValue;
		}
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}
		public double getNumericValue() {
			return numericValue;
		}
		public void setNumericValue(double numericValue) {
			this.numericValue = numericValue;
		}
		
		@Override
		public String toString() {
			if (stringValue == null) return String.valueOf(numericValue);
			return stringValue;
		}
	}
}
