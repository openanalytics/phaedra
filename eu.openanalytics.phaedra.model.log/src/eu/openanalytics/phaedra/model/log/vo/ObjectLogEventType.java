package eu.openanalytics.phaedra.model.log.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="hca_object_log_eventtype", schema="phaedra")
public class ObjectLogEventType {

	@Id
	@Column(name="event_code")
	private String code;
	
	@Column(name="event_label")
	private String label;
	
	@Column(name="event_description")
	private String description;

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
		result = prime * result + ((code == null) ? 0 : code.hashCode());
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
		ObjectLogEventType other = (ObjectLogEventType) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return code + " (" + label + ")";
	}
}
