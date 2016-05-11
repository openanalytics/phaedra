package eu.openanalytics.phaedra.model.protocol.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="hca_welltype", schema="phaedra")
public class WellType implements Comparable<WellType> {

	@Id
	@Column(name="welltype_code")
	private String code;
	
	@Column(name="description")
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
	
	public String toString() {
		return code;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		if (code != null)
			hash = 31 * hash + (code.hashCode());
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
		WellType other = (WellType) obj;
		if (code != other.code)
			return false;
		return true;
	}

	@Override
	public int compareTo(WellType o) {
		if (o == null) return 1;
		return code.compareTo(o.getCode());
	}
}
