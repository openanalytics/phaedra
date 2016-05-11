package eu.openanalytics.phaedra.datacapture.scanner.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import eu.openanalytics.phaedra.base.db.jpa.converter.XMLConverter;

@Entity
@Table(name="hca_dc_scan_job", schema="phaedra")
@Converter(name="xmlConverter", converterClass=XMLConverter.class)
@SequenceGenerator(name="hca_dc_scan_job_s", sequenceName="hca_dc_scan_job_s", schema="phaedra", allocationSize=1)
public class ScanJob {

	@Id
	@Column(name="job_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_dc_scan_job_s")
	private long id;

	@Column(name="schedule")
	private String schedule;

	@Column(name="scanner_type")
	private String type;

	@Column(name="label")
	private String label;

	@Column(name="description")
	private String description;

	@Convert("xmlConverter")
	@Column(name="config")
	private String config;

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

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return label + " (" + id + ")";
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
		ScanJob other = (ScanJob) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
