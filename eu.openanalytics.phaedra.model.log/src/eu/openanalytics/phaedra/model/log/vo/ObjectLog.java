package eu.openanalytics.phaedra.model.log.vo;

import java.util.Date;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

@Entity
@Table(name="hca_object_log", schema="phaedra")
@SequenceGenerator(name="hca_object_log_s", sequenceName="hca_object_log_s", schema="phaedra", allocationSize=1)
public class ObjectLog {

	@Id
	@Column(name="log_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_object_log_s")
	private long log_id;
	
	@Column(name="timestamp")
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;
	
	@Column(name="user_code")
	private String userCode;
	
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="event_code")
	private ObjectLogEventType eventType;
	
	@Column(name="object_class")
	private String objectClass;
	
	@Column(name="object_id")
	private long objectId;
	
	@Column(name="object_prop_1")
	private String objectProperty1;
	
	@Column(name="object_prop_2")
	private String objectProperty2;
	
	@Column(name="old_value")
	private String oldValue;
	
	@Column(name="new_value")
	private String newValue;
	
	@Column(name="remark")
	private String remark;
	
	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */

	public long getLog_id() {
		return log_id;
	}
	public void setLog_id(long log_id) {
		this.log_id = log_id;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getUserCode() {
		return userCode;
	}
	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}
	public ObjectLogEventType getEventType() {
		return eventType;
	}
	public void setEventType(ObjectLogEventType eventType) {
		this.eventType = eventType;
	}
	public String getObjectClass() {
		return objectClass;
	}
	public void setObjectClass(String objectClass) {
		this.objectClass = objectClass;
	}
	public long getObjectId() {
		return objectId;
	}
	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}
	public String getObjectProperty1() {
		return objectProperty1;
	}
	public void setObjectProperty1(String objectProperty1) {
		this.objectProperty1 = objectProperty1;
	}
	public String getObjectProperty2() {
		return objectProperty2;
	}
	public void setObjectProperty2(String objectProperty2) {
		this.objectProperty2 = objectProperty2;
	}
	public String getOldValue() {
		return oldValue;
	}
	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}
	public String getNewValue() {
		return newValue;
	}
	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
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
		result = prime * result + (int) (log_id ^ (log_id >>> 32));
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
		ObjectLog other = (ObjectLog) obj;
		if (log_id != other.log_id)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return eventType + " @ " + timestamp.getTime() + " by " + userCode + ": " + objectClass + "::" + objectId;
	}
}
