package eu.openanalytics.phaedra.datacapture.metrics.model;

import java.util.Date;

public class ServerMetric {

	private long id;
	private Date timestamp;
	
	private long diskUsage;
	private long ramUsage; 
	private double cpu;
	
	private long downloadSpeed;
	private long uploadSpeed;

	public ServerMetric() {
	}

	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public long getDiskUsage() {
		return diskUsage;
	}
	
	public void setDiskUsage(long diskUsage) {
		this.diskUsage = diskUsage;
	}
	
	public long getRamUsage() {
		return ramUsage;
	}
	
	public void setRamUsage(long ramUsage) {
		this.ramUsage = ramUsage;
	}
	
	public double getCpu() {
		return cpu;
	}
	
	public void setCpu(double cpu) {
		this.cpu = cpu;
	}
	
	public long getDownloadSpeed() {
		return downloadSpeed;
	}
	
	public void setDownloadSpeed(long downloadSpeed) {
		this.downloadSpeed = downloadSpeed;
	}
	
	public long getUploadSpeed() {
		return uploadSpeed;
	}
	
	public void setUploadSpeed(long uploadSpeed) {
		this.uploadSpeed = uploadSpeed;
	}
}
