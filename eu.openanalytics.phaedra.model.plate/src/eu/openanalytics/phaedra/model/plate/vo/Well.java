package eu.openanalytics.phaedra.model.plate.vo;

import java.io.Serializable;

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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

@Entity
@Table(name="hca_plate_well", schema="phaedra")
@Cache(size=100000, expiry=3600000)
@SequenceGenerator(name="hca_plate_well_s", sequenceName="hca_plate_well_s", schema="phaedra", allocationSize=1)
public class Well extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = -3668913440466310184L;

	@Id
	@Column(name="well_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_plate_well_s")
	private long id;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="plate_id")
	private Plate plate;

	@Column(name="row_nr")
	private int row;
	@Column(name="col_nr")
	private int column;

	@Column(name="description")
	private String description;

	@Column(name="is_valid")
	private int status;

	@Column(name="welltype_code")
	private String wellType;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.OUTER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="platecompound_id")
	private Compound compound;

	@Column(name="concentration")
	private double compoundConcentration;

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
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	public int getColumn() {
		return column;
	}
	public void setColumn(int column) {
		this.column = column;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getWellType() {
		return wellType;
	}
	public void setWellType(String wellType) {
		this.wellType = wellType;
	}
	public Compound getCompound() {
		return compound;
	}
	public void setCompound(Compound compound) {
		this.compound = compound;
	}
	public double getCompoundConcentration() {
		return compoundConcentration;
	}
	public void setCompoundConcentration(double compoundConcentration) {
		this.compoundConcentration = compoundConcentration;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return "Well " + NumberUtils.getWellCoordinate(row, column) + " (" + id + ")";
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
		Well other = (Well) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getPlate();
	}
}
