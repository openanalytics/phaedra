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
@Table(name="hca_reading", schema="phaedra")
@SequenceGenerator(name="hca_reading_s", sequenceName="hca_reading_s", schema="phaedra", allocationSize=1)
public class PlateReading {

	@Id
	@Column(name="reading_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_reading_s")
	private long id;
	
	@Column(name="reading_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date date;
	
	@Column(name="reading_user")
	private String user;
	
	@Column(name="file_name")
	private String fileName;
	
	@Column(name="file_part")
	private int filePart;
	
	@Column(name="file_info")
	private String fileInfo;
	
	@Column(name="barcode")
	private String barcode;
	
	@Column(name="plate_rows")
	private int rows;
	
	@Column(name="plate_columns")
	private int columns;
	
	@Column(name="src_path")
	private String sourcePath;

	@Column(name="capture_path")
	private String capturePath;
	
	@Column(name="instrument")
	private String instrument;
	@Column(name="protocol")
	private String protocol;
	@Column(name="experiment")
	private String experiment;
	
	@Column(name="link_dt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date linkDate;

	@Column(name="link_user")
	private String linkUser;

	@Column(name="link_status")
	private int linkStatus;

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

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getFilePart() {
		return filePart;
	}

	public void setFilePart(int filePart) {
		this.filePart = filePart;
	}

	public String getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(String fileInfo) {
		this.fileInfo = fileInfo;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
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
	
	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getCapturePath() {
		return capturePath;
	}
	
	public void setCapturePath(String capturePath) {
		this.capturePath = capturePath;
	}
	
	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getExperiment() {
		return experiment;
	}
	
	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}
	
	public Date getLinkDate() {
		return linkDate;
	}

	public void setLinkDate(Date linkDate) {
		this.linkDate = linkDate;
	}

	public String getLinkUser() {
		return linkUser;
	}

	public void setLinkUser(String linkUser) {
		this.linkUser = linkUser;
	}

	public int getLinkStatus() {
		return linkStatus;
	}

	public void setLinkStatus(int linkStatus) {
		this.linkStatus = linkStatus;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public String toString() {
		return barcode + " (" + id + ")";
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
		PlateReading other = (PlateReading) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
