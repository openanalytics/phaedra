package eu.openanalytics.phaedra.datacapture.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="hca_dc_log", schema="phaedra")
@SequenceGenerator(name="hca_dc_log_s", sequenceName="hca_dc_log_s", schema="phaedra", allocationSize=1)
public class SavedLogEvent {

	@Id
	@Column(name="log_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_dc_log_s")
	private long id;
	
	@Column(name="log_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date date;
	
	@Column(name="log_source")
	private String source;
	
	@Column(name="status_code")
	private int status;
	
	@Column(name="message")
	private String message;
	
	@Column(name="error")
	private String error;
	
	@Column(name="source_path")
	private String sourcePath;
	
	@Column(name="source_identifier")
	private String sourceIdentifier;
	
	@Column(name="reading")
	private String reading;

	@Column(name="task_id")
	private String taskId;
	
	@Column(name="task_user")
	private String taskUser;
	
	@Column(name="dc_server_id")
	private String serverId;
	
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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSourceIdentifier() {
		return sourceIdentifier;
	}
	
	public void setSourceIdentifier(String sourceIdentifier) {
		this.sourceIdentifier = sourceIdentifier;
	}
	
	public String getReading() {
		return reading;
	}

	public void setReading(String reading) {
		this.reading = reading;
	}

	public String getTaskId() {
		return taskId;
	}
	
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	
	public String getTaskUser() {
		return taskUser;
	}
	
	public void setTaskUser(String taskUser) {
		this.taskUser = taskUser;
	}
	
	public String getServerId() {
		return serverId;
	}
	
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	
	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public String toString() {
		return date.toString() + "\t(" + status + ")\t" + message;
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
		SavedLogEvent other = (SavedLogEvent) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}
