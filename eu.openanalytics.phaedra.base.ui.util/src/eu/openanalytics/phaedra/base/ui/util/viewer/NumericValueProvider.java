package eu.openanalytics.phaedra.base.ui.util.viewer;

import org.eclipse.jface.viewers.ILabelProvider;


/**
 * Label provider also providing a percentage value e.g. for conditional rendering
 */
public interface NumericValueProvider extends ILabelProvider {
	
	
	double getNumericValue(Object element);
	
}
