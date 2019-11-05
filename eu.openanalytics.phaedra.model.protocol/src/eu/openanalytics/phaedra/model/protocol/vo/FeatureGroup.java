package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;

@Entity
@Table(name="hca_feature_group", schema="phaedra")
@SequenceGenerator(name="hca_feature_group_s", sequenceName="hca_feature_group_s", schema="phaedra", allocationSize=1)
public class FeatureGroup extends PlatformObject implements IValueObject, IOwnedObject, Serializable {

	private static final long serialVersionUID = -4584184655696859819L;

	@Id
	@Column(name="group_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_feature_group_s")
	private long id;

	@Column(name="group_name")
	private String name;
	@Column(name="description")
	private String description;

	@ManyToOne
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;

	@Column(name="group_type")
	private int type;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}

	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

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
		FeatureGroup other = (FeatureGroup) obj;
		if (id == 0l)
			return getName().equals(other.getName()) && getType() == other.getType();
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
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