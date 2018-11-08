package eu.openanalytics.phaedra.ui.perspective.vo;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.security.model.IOwnedPersonalObject;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

@Entity
@Table(name="hca_psp", schema="phaedra")
@SequenceGenerator(name="hca_psp_s", sequenceName="hca_psp_s", schema="phaedra", allocationSize=1)
public class SavedPerspective extends PlatformObject implements IOwnedPersonalObject, IValueObject {

	@Id
	@Column(name="psp_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_psp_s")
	private long id;

	@Column(name="psp_name")
	private String name;

	@Column(name="workbench_state")
	private String workbenchState;

	@Column(name="owner")
	private String owner;

	@Column(name="access_scope")
	@Enumerated(EnumType.STRING)
	private AccessScope accessScope;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="feature_id")
	private Feature feature;

	@IgnoreSizeOf
	@OneToMany(mappedBy="perspective", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<SavedPartReference> savedParts;

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

	public String getWorkbenchState() {
		return workbenchState;
	}

	public void setWorkbenchState(String workbenchState) {
		this.workbenchState = workbenchState;
	}

	public Feature getFeature() {
		return feature;
	}

	public void setFeature(Feature feature) {
		this.feature = feature;
	}

	@Override
	public AccessScope getAccessScope() {
		return accessScope;
	}

	public void setAccessScope(AccessScope accessScope) {
		this.accessScope = accessScope;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public List<SavedPartReference> getSavedParts() {
		return savedParts;
	}

	public void setSavedParts(List<SavedPartReference> savedParts) {
		this.savedParts = savedParts;
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
	public IValueObject getParent() {
		return feature.getProtocolClass();
	}

	@Override
	public String[] getOwners() {
		return feature.getProtocolClass().getOwners();
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
		SavedPerspective other = (SavedPerspective) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
