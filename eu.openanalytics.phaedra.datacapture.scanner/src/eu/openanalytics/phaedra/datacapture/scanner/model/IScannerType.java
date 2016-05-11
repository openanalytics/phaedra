package eu.openanalytics.phaedra.datacapture.scanner.model;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.datacapture.scanner.Activator;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;

public interface IScannerType {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".scannerType";
	public final static String ATTR_ID = "id";
	public final static String ATTR_CLASS = "class";
	
	/**
	 * Get the ID of this scanner type, as defined by its extension.
	 * 
	 * @return The ID of this scanner type.
	 */
	public String getId();
	
	/**
	 * Execute the given scanner.
	 * If the scanner type of the scanner does not match this scanner type, a ScanException will be thrown.
	 * 
	 * @param scanJob The Scanner to execute.
	 * @param monitor A progress monitor.
	 * @throws ScanException If the scan fails for any reason.
	 */
	public void run(ScanJob scanJob, IProgressMonitor monitor) throws ScanException;
	
}
