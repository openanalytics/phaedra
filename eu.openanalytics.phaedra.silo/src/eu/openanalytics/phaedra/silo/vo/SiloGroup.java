package eu.openanalytics.phaedra.silo.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.security.model.IOwnedPersonalObject;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

@Entity
@Table(name="hca_silo_group", schema="phaedra")
@SequenceGenerator(name="hca_silo_group_s", sequenceName="hca_silo_group_s", schema="phaedra", allocationSize=1)
public class SiloGroup extends PlatformObject implements IOwnedPersonalObject, IValueObject, Serializable {

	private static final long serialVersionUID = 632443134699272798L;

	@Id
	@Column(name="group_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_silo_group_s")
	private long id;

	@Column(name="group_name")
	private String name;
	@Column(name="description")
	private String description;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;

	@Column(name="owner")
	private String owner;

	@Column(name="creation_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationDate;

	@Column(name="group_type")
	private int type;

	@Column(name="access_scope")
	@Enumerated(EnumType.STRING)
	private AccessScope accessScope;

	@Column(name="is_example")
	private boolean isExample;

	@IgnoreSizeOf
	@ManyToMany(mappedBy = "siloGroups")
	private Set<Silo> silos;

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

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public AccessScope getAccessScope() {
		return accessScope;
	}

	public void setAccessScope(AccessScope accessScope) {
		this.accessScope = accessScope;
	}

	public boolean isExample() {
		return isExample;
	}

	public void setExample(boolean isExample) {
		this.isExample = isExample;
	}

	public Set<Silo> getSilos() {
		return silos;
	}

	public void setSilos(Set<Silo> silos) {
		this.silos = silos;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

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
		SiloGroup other = (SiloGroup) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getProtocolClass();
	}

	@Override
	public String[] getOwners() {
		return getProtocolClass().getOwners();
	}

}
