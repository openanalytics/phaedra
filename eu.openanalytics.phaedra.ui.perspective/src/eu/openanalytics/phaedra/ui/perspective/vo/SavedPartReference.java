package eu.openanalytics.phaedra.ui.perspective.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;


@Entity
@Table(name="hca_psp_part_ref", schema="phaedra")
@SequenceGenerator(name="hca_psp_part_ref_s", sequenceName="hca_psp_part_ref_s", schema="phaedra", allocationSize=1)
public class SavedPartReference {

	@Id
	@Column(name="part_ref_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_psp_part_ref_s")
	private long id;
	
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="psp_id")
	private SavedPerspective perspective;

	@Column(name="part_id")
	private String partId;
	
	@Column(name="part_secondary_id")
	private String secondaryId;
	
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="part_settings_id")
	private PartSettings partSettings;

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getPartId() {
		return partId;
	}

	public void setPartId(String partId) {
		this.partId = partId;
	}

	public SavedPerspective getPerspective() {
		return perspective;
	}

	public void setPerspective(SavedPerspective perspective) {
		this.perspective = perspective;
	}
	
	public PartSettings getPartSettings() {
		return partSettings;
	}
	
	public void setPartSettings(PartSettings partSettings) {
		this.partSettings = partSettings;
	}
	
	public String getSecondaryId() {
		return secondaryId;
	}
	
	public void setSecondaryId(String secondaryId) {
		this.secondaryId = secondaryId;
	}
	
	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public String toString() {
		return "Part Ref " + partId + " (PSP " + perspective.getId() + ")";
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
		SavedPartReference other = (SavedPartReference) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
