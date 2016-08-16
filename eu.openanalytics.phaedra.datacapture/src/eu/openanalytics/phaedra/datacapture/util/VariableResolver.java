package eu.openanalytics.phaedra.datacapture.util;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.config.ParameterGroup;
import eu.openanalytics.phaedra.datacapture.module.IModule;

public class VariableResolver {

	private final static String READING_PREFIX = "reading.";

	/**
	 * Attempt to resolve a parameter.
	 * The resolution order is:
	 * <ol>
	 * <li>If the key starts with "reading.", look in the current reading's runtime parameters</li>
	 * <li>Else, look in the DataCaptureTask parameters</li>
	 * <li>Else, look in the current module's parameters</li>
	 * <li>Else, look in the capture's global parameters</li>
	 * </ol>
	 * 
	 * @param key The parameter name to resolve.
	 * @return The matching value, or null if no value was found.
	 */
	public static Object get(String key) {
		DataCaptureContext ctx = DataCaptureContext.getCurrent();
		if (key == null || key.isEmpty() || ctx == null) return null;

		Object value = null;
		
		if (ctx.getActiveReading() != null) {
			if (key.equals("barcode") || key.equals(READING_PREFIX + ".barcode")) return ctx.getActiveReading().getBarcode();
			else if (key.startsWith(READING_PREFIX)) {
				// Look in the reading-specific parameters (DataCaptureContext)
				ParameterGroup params = ctx.getParameters(ctx.getActiveReading());
				if (params != null) value = params.getParameter(key.substring(READING_PREFIX.length()));
			}
		} else {
			// 1. Look in task parameters (i.e. "runtime" parameters)
			value = ctx.getTask().getParameters().get(key);
			
			IModule activeModule = ctx.getActiveModule();
			if (activeModule != null) {
				// 2. Look in module parameters
				if (value == null) value = activeModule.getConfig().getParameters().getParameter(key);
				
				// 3. Look in global parameters
				if (value == null) value = activeModule.getConfig().getParentConfig().getParameters().getParameter(key);
			}
		}
		
		return value;
	}
}
