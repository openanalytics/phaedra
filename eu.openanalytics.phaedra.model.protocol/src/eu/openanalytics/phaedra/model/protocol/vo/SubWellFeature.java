package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_subwellfeature", schema="phaedra")
@SequenceGenerator(name="hca_subwellfeature_s", sequenceName="hca_subwellfeature_s", schema="phaedra", allocationSize=1)
public class SubWellFeature extends PlatformObject implements IFeature, Serializable {

	private static final long serialVersionUID = 626557857179594694L;

	@Id
	@Column(name="subwellfeature_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_subwellfeature_s")
	private long id;

	@Column(name="subwellfeature_name")
	private String name;
	@Column(name="short_name")
	private String shortName;
	@Column(name="description")
	private String description;

	@ManyToOne
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;

	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToMany(mappedBy="subWellFeature", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<FeatureClass> featureClasses;

	@Column(name="is_numeric")
	private boolean numeric;
	@Column(name="is_logarithmic")
	private boolean logarithmic;
	@Column(name="is_key")
	private boolean key;

	@Column(name="format_string")
	private String formatString;

	@Column(name="position_role")
	private String positionRole;

	@JoinFetch(JoinFetchType.OUTER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="group_id")
	private FeatureGroup featureGroup;

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getFormatString() {
		return formatString;
	}

	public void setFormatString(String formatString) {
		this.formatString = formatString;
	}

	@Override
	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}

	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}

	@Override
	public List<FeatureClass> getFeatureClasses() {
		return featureClasses;
	}

	public void setFeatureClasses(List<FeatureClass> featureClasses) {
		this.featureClasses = featureClasses;
	}

	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

	@Override
	public boolean isNumeric() {
		return numeric;
	}

	public void setLogarithmic(boolean logarithmic) {
		this.logarithmic = logarithmic;
	}

	public boolean isLogarithmic() {
		return logarithmic;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

	@Override
	public boolean isKey() {
		return key;
	}

	public String getPositionRole() {
		return positionRole;
	}

	public void setPositionRole(String positionRole) {
		this.positionRole = positionRole;
	}

	public void setFeatureGroup(FeatureGroup featureGroup) {
		this.featureGroup = featureGroup;
	}

	@Override
	public FeatureGroup getFeatureGroup() {
		return featureGroup;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String getDisplayName() {
		if (shortName != null && !shortName.isEmpty()) {
			return shortName;
		}
		return name;
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
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
		SubWellFeature other = (SubWellFeature) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String[] getOwners() {
		ProtocolClass pClass = getProtocolClass();
		if (pClass != null) return pClass.getOwners();
		return new String[0];
	}

	@Override
	public IValueObject getParent() {
		return getProtocolClass();
	}
}