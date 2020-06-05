package eu.openanalytics.phaedra.model.plate.vo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

@Entity
@Table(name="hca_experiment", schema="phaedra")
@SequenceGenerator(name="hca_experiment_s", sequenceName="hca_experiment_s", schema="phaedra", allocationSize=1)
public class Experiment extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = 6779503035193011735L;

	@Id
	@Column(name="experiment_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_experiment_s")
	private long id;

	@IgnoreSizeOf
	@ManyToOne
	@BatchFetch(BatchFetchType.JOIN)
	@JoinColumn(name="protocol_id")
	private Protocol protocol;

	@Column(name="experiment_name")
	private String name;
	
	@Column(name="multiplo_method")
	private String multiploMethod;
	
	@Column(name="multiplo_parameter")
	private String multiploParameter;
	
	@Column(name="description")
	private String description;
	
	@Column(name="closed")
	private boolean closed;
	
	@Column(name="comments")
	private String comments;

	@Column(name="experiment_user")
	private String creator;

	@Column(name="experiment_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate;

	@Column(name="archive_status")
	private int archiveStatus;

	@Column(name="archive_user")
	private String archiveUser;

	@Column(name="archive_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date archiveDate;
	
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

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getMultiploMethod() {
		return multiploMethod;
	}
	
	public void setMultiploMethod(String multiploMethod) {
		this.multiploMethod = multiploMethod;
	}
	
	public String getMultiploParameter() {
		return multiploParameter;
	}
	
	public void setMultiploParameter(String multiploParameter) {
		this.multiploParameter = multiploParameter;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isClosed() {
		return this.closed;
	}
	
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public int getArchiveStatus() {
		return archiveStatus;
	}

	public void setArchiveStatus(int archiveStatus) {
		this.archiveStatus = archiveStatus;
	}

	public String getArchiveUser() {
		return archiveUser;
	}

	public void setArchiveUser(String archiveUser) {
		this.archiveUser = archiveUser;
	}

	public Date getArchiveDate() {
		return archiveDate;
	}

	public void setArchiveDate(Date archiveDate) {
		this.archiveDate = archiveDate;
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
		Experiment other = (Experiment) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getProtocol();
	}
}
