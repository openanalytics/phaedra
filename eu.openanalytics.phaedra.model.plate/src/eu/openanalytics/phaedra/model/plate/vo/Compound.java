package eu.openanalytics.phaedra.model.plate.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.IListAdaptable;

@Entity
@Table(name="hca_plate_compound", schema="phaedra")
@Cache(size=10000, expiry=3600000)
@SequenceGenerator(name="hca_plate_compound_s", sequenceName="hca_plate_compound_s", schema="phaedra", allocationSize=1)
public class Compound extends PlatformObject implements IValueObject, IListAdaptable, Serializable {

	private static final long serialVersionUID = -1115498342680368047L;

	@Id
	@Column(name="platecompound_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_plate_compound_s")
	private long id;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="plate_id")
	private Plate plate;

	@Column(name="compound_ty")
	private String type;
	@Column(name="compound_nr")
	private String number;

	@Column(name="saltform")
	private String saltform;

	@Column(name="description")
	private String description;

	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToMany(mappedBy="compound")
	private List<Well> wells;

	@Column(name="validate_status")
	private int validationStatus;
	@Column(name="validate_user")
	private String validationUser;
	@Column(name="validate_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date validationDate;

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
	public Plate getPlate() {
		return plate;
	}
	public void setPlate(Plate plate) {
		this.plate = plate;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public String getSaltform() {
		return saltform;
	}
	public void setSaltform(String saltform) {
		this.saltform = saltform;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Well> getWells() {
		return wells;
	}
	public void setWells(List<Well> wells) {
		this.wells = wells;
	}
	public int getValidationStatus() {
		return validationStatus;
	}
	public void setValidationStatus(int validationStatus) {
		this.validationStatus = validationStatus;
	}
	public String getValidationUser() {
		return validationUser;
	}
	public void setValidationUser(String validationUser) {
		this.validationUser = validationUser;
	}
	public Date getValidationDate() {
		return validationDate;
	}
	public void setValidationDate(Date validationDate) {
		this.validationDate = validationDate;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public <T> List<T> getAdapterList(Class<T> type) {
		if (type == Well.class) {
			return (List<T>) getWells();
		}
		return null;
	}
	
	
	@Override
	public String toString() {
		return type + " " + number;
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
		Compound other = (Compound) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getPlate();
	}
}
