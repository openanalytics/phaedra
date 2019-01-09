package eu.openanalytics.phaedra.model.plate.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
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

@Entity
@Table(name="hca_plate", schema="phaedra")
@Cache(size=1000, expiry=3600000)
@SequenceGenerator(name="hca_plate_s", sequenceName="hca_plate_s", schema="phaedra", allocationSize=1)
public class Plate extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = 764113069395604585L;

	@Id
	@Column(name="plate_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_plate_s")
	private long id;

	@Column(name="plate_rows")
	private int rows;
	@Column(name="plate_columns")
	private int columns;

	@IgnoreSizeOf
	@OneToMany(mappedBy="plate", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<Well> wells;

	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToMany(mappedBy="plate") // Let DB handle cascade. If compounds are removed before wells, constraint errors will occur.
	private List<Compound> compounds;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="experiment_id")
	private Experiment experiment;

	@Column(name="barcode")
	private String barcode;
	@Column(name="barcode_source")
	private String barcodeSource;
	@Column(name="sequence_in_run")
	private int sequence;

	@Column(name="description")
	private String description;
	@Column(name="plate_info")
	private String info;

	@Column(name="jpx_path")
	private String imagePath;
	@Column(name="jpx_available")
	private boolean imageAvailable;

	@Column(name="celldata_available")
	private boolean subWellDataAvailable;

	@Column(name="calc_status")
	private int calculationStatus;
	@Column(name="calc_error")
	private String calculationError;
	@Column(name="calc_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date calculationDate;

	@Column(name="validate_status")
	private int validationStatus;
	@Column(name="validate_user")
	private String validationUser;
	@Column(name="validate_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date validationDate;

	@Column(name="approve_status")
	private int approvalStatus;
	@Column(name="approve_user")
	private String approvalUser;
	@Column(name="approve_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date approvalDate;

	@Column(name="upload_status")
	private int uploadStatus;
	@Column(name="upload_user")
	private String uploadUser;
	@Column(name="upload_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date uploadDate;

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
	public int getRows() {
		return rows;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	public int getColumns() {
		return columns;
	}
	public void setColumns(int columns) {
		this.columns = columns;
	}
	public List<Well> getWells() {
		return wells;
	}
	public void setWells(List<Well> wells) {
		this.wells = wells;
	}
	public List<Compound> getCompounds() {
		return compounds;
	}
	public void setCompounds(List<Compound> compounds) {
		this.compounds = compounds;
	}
	public Experiment getExperiment() {
		return experiment;
	}
	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
	}
	public String getBarcode() {
		return barcode;
	}
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}
	public String getBarcodeSource() {
		return barcodeSource;
	}
	public void setBarcodeSource(String barcodeSource) {
		this.barcodeSource = barcodeSource;
	}
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public String getImagePath() {
		return imagePath;
	}
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	public boolean isImageAvailable() {
		return imageAvailable;
	}
	public void setImageAvailable(boolean imageAvailable) {
		this.imageAvailable = imageAvailable;
	}
	public boolean isSubWellDataAvailable() {
		return subWellDataAvailable;
	}
	public void setSubWellDataAvailable(boolean subWellDataAvailable) {
		this.subWellDataAvailable = subWellDataAvailable;
	}
	public int getCalculationStatus() {
		return calculationStatus;
	}
	public void setCalculationStatus(int calculationStatus) {
		this.calculationStatus = calculationStatus;
	}
	public String getCalculationError() {
		return calculationError;
	}
	public void setCalculationError(String calculationError) {
		this.calculationError = calculationError;
	}
	public Date getCalculationDate() {
		return calculationDate;
	}
	public void setCalculationDate(Date calculationDate) {
		this.calculationDate = calculationDate;
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
	public int getApprovalStatus() {
		return approvalStatus;
	}
	public void setApprovalStatus(int approvalStatus) {
		this.approvalStatus = approvalStatus;
	}
	public String getApprovalUser() {
		return approvalUser;
	}
	public void setApprovalUser(String approvalUser) {
		this.approvalUser = approvalUser;
	}
	public Date getApprovalDate() {
		return approvalDate;
	}
	public void setApprovalDate(Date approvalDate) {
		this.approvalDate = approvalDate;
	}
	public int getUploadStatus() {
		return uploadStatus;
	}
	public void setUploadStatus(int uploadStatus) {
		this.uploadStatus = uploadStatus;
	}
	public String getUploadUser() {
		return uploadUser;
	}
	public void setUploadUser(String uploadUser) {
		this.uploadUser = uploadUser;
	}
	public Date getUploadDate() {
		return uploadDate;
	}
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return "Plate " + barcode + " (" + id + ")";
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
		Plate other = (Plate) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getExperiment();
	}
}
