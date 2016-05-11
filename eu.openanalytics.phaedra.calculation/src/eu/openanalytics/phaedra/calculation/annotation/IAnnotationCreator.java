package eu.openanalytics.phaedra.calculation.annotation;

import java.util.Map;
import java.util.Set;

import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public interface IAnnotationCreator {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".annotationCreator";
	public final static String ATTR_CLASS = "class";
	
	/**
	 * Get the priority this creator has with regard to other creators.
	 * A value of zero means the creator should not be used in the current circumstances.
	 * A high value means the creator is well suited in the current circumstances.
	 * 
	 * @return A priority value, not negative.
	 */
	public int getPriority();
	
	/**
	 * Create the given set of annotations, optionally with a number of predefined values.
	 * Instead of creating the annotations, this method may also throw an exception, for example if
	 * the user does not have permission to modify the protocol class, or if other prerequisites are not met.
	 * 
	 * @param pClass The protocol class where the annotations should be created.
	 * @param annotationsAndValues The undefined annotations. For each annotation, an optional set of predefined values.
	 * @param annotationsNumeric For each annotation, true if it should be numeric, false otherwise.
	 */
	public void create(ProtocolClass pClass, Map<String, Set<String>> annotationsAndValues, Map<String, Boolean> annotationsNumeric);
	
}
