package eu.openanalytics.phaedra.datacapture.metrics.internal;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.metrics.Activator;

public class ServerMetrics {

	private static Sigar sigar;
	private static long timestamp = 0;
	private static long diskMemTotal = 0;
	private static long diskMemUsed = 0;
	private static long physicalMemTotal = 0;
	private static long physicalMemUsed = 0;
	private static double cpuUsed = 0;
	
	private static void checkSigar() {
		if(sigar == null) sigar = new Sigar();
	}
	
	public static void setSigar(Sigar sigarInstance) {
		if(sigar == null) sigar = sigarInstance;
		checkSigar();
	}
	
	private static void checkLastUpdate() {
    	if(timestamp < (System.currentTimeMillis() - 1000)) {
    		boolean collected = collectMetrics();
    		if(!collected) {
    			sigar = new Sigar();
    			collectMetrics();
    		}
        	timestamp = System.currentTimeMillis();
    	}
    }
	
	private static boolean collectMetrics() {
		checkSigar();
		try {
			diskMetrics();
			physicalMemMetrics();
			cpuMetrics();
			return true;
		} catch (SigarException e) {
        	EclipseLog.error("Server Metrics sigar exception: "+e.getMessage(), e, Activator.getDefault());
        	return false;
		}
	}
	
	private static void diskMetrics() throws SigarException {
		FileSystemUsage fileSystemUsage = sigar.getFileSystemUsage(System.getProperty("java.io.tmpdir"));
		diskMemTotal = fileSystemUsage.getTotal();
		diskMemUsed =  fileSystemUsage.getUsed();
	}
	
	private static void physicalMemMetrics() throws SigarException {
		Mem	physicalMemory = sigar.getMem();
		physicalMemTotal = physicalMemory.getRam() * 1024 * 1024;
		physicalMemUsed = physicalMemory.getActualUsed();
	}
	
	private static void cpuMetrics() throws SigarException {
		CpuPerc	cpu = sigar.getCpuPerc();
		cpuUsed = cpu.getCombined() * 100;
	}
	
	public static long getDiskTotal() {
		if(diskMemTotal <= 0) checkLastUpdate();
		return diskMemTotal <= 0? 1 : diskMemTotal;
	}
	
	public static long getDiskUsed() {
		checkLastUpdate();
		return diskMemUsed;
	}
	
	public static long getPhysicalTotal() {
		if(physicalMemTotal <= 0) checkLastUpdate();
		return physicalMemTotal <= 0? 1 : physicalMemTotal;
	}
	
	public static long getPhysicalUsed() {
		checkLastUpdate();
		return physicalMemUsed;
	}
	
	public static double getCpu() {
		checkLastUpdate();
		return cpuUsed;
	}

}
