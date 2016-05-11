package eu.openanalytics.phaedra.datacapture.metrics.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.metrics.Activator;

public class NetworkMetrics {

    static Map<String, Long> rxCurrentMap = new HashMap<String, Long>();
    static Map<String, List<Long>> rxChangeMap = new HashMap<String, List<Long>>();
    static Map<String, Long> txCurrentMap = new HashMap<String, Long>();
    static Map<String, List<Long>> txChangeMap = new HashMap<String, List<Long>>();
    private static Sigar sigar;
    static long timestamp = 0;
    static boolean updating = false;
    
    private static final long INTERVAL = 4000;

    // Network upload (totaltx) and download (totalrx)
    private static long totaltx = 0;
    private static long totalrx = 0;
    private static long bandwidth = 0;
	
    private static void checkLastUpdate() {
    	if(timestamp < (System.currentTimeMillis() - INTERVAL)) {
    		if(!updating){
	    		updating = true;
	    		timestamp = System.currentTimeMillis();
	    		checkSigar();
	    		Runnable myRunnable = new Runnable() {
	                public void run() {
	                    try {
	                    	NetworkMetrics.networkMetricThread();
	                    } catch (Exception e) {
	                        // TODO decent exception
	                    }
	                }
	            };
	            new Thread(myRunnable, "networkMetricsCollector").start();
    		}
    	}
    	if(updating) {
			try {
				Thread.sleep(1020);
				while(updating) {
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {}
    	}
		timestamp = System.currentTimeMillis();
    }
    
	private static void checkSigar() {
		if(sigar == null) sigar = new Sigar();
	}
	
	public static void setSigar(Sigar sigarInstance) {
		if(sigar == null) sigar = sigarInstance;
		checkSigar();
	}
	
    public static void networkMetricThread() {
        while (true) {
        	boolean collected = collectMetrics();
        	if(!collected) {
        		sigar = new Sigar();
        		collected = collectMetrics();
        		if(!collected){
        			updating = false;
        			timestamp = 0;
        			break;
        		}
        	}
            try {
            	Thread.sleep(1000);
            } catch (InterruptedException e) {}
            updating = false;
            if(timestamp < (System.currentTimeMillis() - INTERVAL)) {
                break;
            }
        }
    }
    
    private static boolean collectMetrics() {
    	try {
    		getMetric();
    		return true;
    	} catch (SigarException e) {
        	EclipseLog.error("Network Metric sigar exception: "+e.getMessage(), e, Activator.getDefault());
    		return false;
    	}
    }
	
	private static void getMetric() throws SigarException {
    	List<Long> speed = new ArrayList<Long>();
        for (String ni : sigar.getNetInterfaceList()) {
            NetInterfaceStat netStat = sigar.getNetInterfaceStat(ni);
            NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig(ni);
            String hwaddr = null;
            if (!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr())) {
                hwaddr = ifConfig.getHwaddr();
            }
            if (hwaddr != null) {
            	speed.add(netStat.getSpeed());
                long rxCurrenttmp = netStat.getRxBytes();
                saveChange(rxCurrentMap, rxChangeMap, hwaddr, rxCurrenttmp, ni);
                long txCurrenttmp = netStat.getTxBytes();
                saveChange(txCurrentMap, txChangeMap, hwaddr, txCurrenttmp, ni);
            }
        }
        totalrx = getMetricData(rxChangeMap) *8;
        totaltx = getMetricData(txChangeMap) *8;
        bandwidth = Collections.max(speed);
        for (List<Long> l : rxChangeMap.values())
            l.clear();
        for (List<Long> l : txChangeMap.values())
            l.clear();
    }

    private static long getMetricData(Map<String, List<Long>> rxChangeMap) {
        long total = 0;
        for (Entry<String, List<Long>> entry : rxChangeMap.entrySet()) {
            int average = 0;
            for (Long l : entry.getValue()) {
                average += l;
            }
            int size = entry.getValue().size() > 1? entry.getValue().size() : 1;
            total += average / size;
        }
        return total;
    }

    private static void saveChange(Map<String, Long> currentMap,
            Map<String, List<Long>> changeMap, String hwaddr, long current,
            String ni) {
        Long oldCurrent = currentMap.get(ni);
        if (oldCurrent != null) {
            List<Long> list = changeMap.get(hwaddr);
            if (list == null) {
                list = new LinkedList<Long>();
                changeMap.put(hwaddr, list);
            }
            list.add(current - oldCurrent);
        }
        currentMap.put(ni, current);
    }

    public static long getBandwidth() {
    	if(bandwidth <= 0) checkLastUpdate();
    	return bandwidth <= 0? 1 : bandwidth;
    }
    
    public static long getDownloadSpeed() {
    	checkLastUpdate();
    	return totalrx;
    }
    
    public static long getUploadSpeed() {
    	checkLastUpdate();
    	return totaltx;
    }
    
}
