package eu.openanalytics.phaedra.datacapture.module;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;

public interface IModule {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".module";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_TYPE = "type";
	
	public String getType();
	public String getName();
	public String getId();
	public ModuleConfig getConfig();
	
	/**
	 * This method is called at the start of a data capture job,
	 * before any modules are actually executed.
	 * 
	 * @param cfg The module configuration.
	 * @param task The data capture task description.
	 * @throws DataCaptureException If the configuration fails. This will abort the data capture job.
	 */
	public void configure(ModuleConfig cfg) throws DataCaptureException;
	
	/**
	 * Execute this data capture module.
	 * At this point, all modules have been configured, and the previous modules have been executed successfully.
	 * 
	 * @param context The data capture context, containing the (possibly incomplete) readings captured so far.
	 * @param monitor The progress monitor, for reporting progress and logging information.
	 * @throws DataCaptureException If the module execution fails. This will abort the data capture job.
	 */
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException;
	
	/**
	 * This method is called at the end of a data capture job.
	 * The job may have been successful, or it may have been cancelled or encountered an error:
	 * this method is called regardless of the outcome.
	 * In the case of a cancel or error, it is called even if this module was not yet executed.
	 * 
	 * @param context The data capture context, containing the (possibly incomplete) readings captured.
	 * @param monitor The data capture monitor, for reporting progress and logging information.
	 */
	public void postCapture(DataCaptureContext context, IProgressMonitor monitor);
	
	/**
	 * Estimate the 'weight' of this module, i.e. the amount of time it needs to process one reading.
	 * A weight of 1 means it is almost instant, such as renaming a barocde.
	 * A weight of 100 means it takes a lot of time to complete, such as image compression. 
	 * 
	 * @return A weight between 1 and 100.
	 */
	public int getWeight();
}
