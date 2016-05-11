package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;

@Entity
@Table(name="hca_protocol", schema="phaedra")
@SequenceGenerator(name="hca_protocol_s", sequenceName="hca_protocol_s", schema="phaedra", allocationSize=1)
public class Protocol extends PlatformObject implements IValueObject, IOwnedObject, Serializable {

	private static final long serialVersionUID = 2462043458391226041L;

	@Id
	@Column(name="protocol_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_protocol_s")
	private long id;

	@Column(name="protocol_name")
	private String name;
	@Column(name="description")
	private String description;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;

	@IgnoreSizeOf
	@OneToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="image_setting_id")
	private ImageSettings imageSettings;

	@Column(name="team_code")
	private String teamCode;

	@Column(name="upload_system")
	private String uploadSystem;

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

	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}

	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}

	public ImageSettings getImageSettings() {
		return imageSettings;
	}

	public void setImageSettings(ImageSettings imageSettings) {
		this.imageSettings = imageSettings;
	}

	public String getTeamCode() {
		return teamCode;
	}

	public void setTeamCode(String teamCode) {
		this.teamCode = teamCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUploadSystem() {
		return uploadSystem;
	}

	public void setUploadSystem(String uploadSystem) {
		this.uploadSystem = uploadSystem;
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
		int hash = 7;
		hash = 31 * hash + +(int) (id ^ (id >>> 32));
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
		Protocol other = (Protocol) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String[] getOwners() {
		if (getTeamCode() == null) return new String[0];
		return new String[]{getTeamCode()};
	}

	@Override
	public IValueObject getParent() {
		return getProtocolClass();
	}
}
