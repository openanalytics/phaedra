package eu.openanalytics.phaedra.base.util;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;


/**
 * Like {@link IAdaptable} but allows to provide a list with adapters.
 */
public interface IListAdaptable extends IAdaptable {
	
	
	<T> List<T> getAdapterList(Class<T> type);
	
}
