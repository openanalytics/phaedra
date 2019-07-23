package eu.openanalytics.phaedra.project.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.security.model.IOwnedPersonalObject;

@Entity
@Table(name="hca_project", schema="phaedra")
@SequenceGenerator(name="hca_project_s", sequenceName="hca_project_s", schema="phaedra", allocationSize=1)
public class Project extends PlatformObject implements IOwnedPersonalObject, IValueObject, Serializable {

	private static final long serialVersionUID = -1430694889362541398L;


	@Id
	@Column(name="project_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_project_s")
	private long id;

	@Column(name="name")
	private String name;
	@Column(name="description")
	private String description;

	@Column(name="owner")
	private String owner;
	@Column(name="team_code")
	private String teamCode;

	@Column(name="access_scope")
	@Enumerated(EnumType.STRING)
	private AccessScope accessScope;


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


	@Override
	public String getOwner() {
		return owner;
	}
	
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getTeamCode() {
		return teamCode;
	}
	
	public void setTeamCode(String teamCode) {
		this.teamCode = teamCode;
	}

	@Override
	public AccessScope getAccessScope() {
		return accessScope;
	}
	
	public void setAccessScope(AccessScope accessScope) {
		this.accessScope = accessScope;
	}


	@Override
	public IValueObject getParent() {
		return null;
	}

	@Override
	public String[] getOwners() {
		String teamCode = getTeamCode();
		return (teamCode != null) ? new String[] { teamCode } : new String[0];
	}


	@Override
	public String toString() {
		return name + " (" + id + ")";
	}

	@Override
	public int hashCode() {
		int hash = 41;
		hash = 31 * hash + (int) (id ^ (id >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Project other = (Project) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
