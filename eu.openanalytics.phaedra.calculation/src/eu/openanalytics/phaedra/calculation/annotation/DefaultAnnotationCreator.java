package eu.openanalytics.phaedra.calculation.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class DefaultAnnotationCreator implements IAnnotationCreator {

	@Override
	public int getPriority() {
		return 1;
	}
	
	@Override
	public void create(ProtocolClass pClass, Map<String, Set<String>> annotationsAndValues, Map<String, Boolean> annotationsNumeric) {
		for (String annotation: annotationsAndValues.keySet()) {
			List<FeatureClass> classes = new ArrayList<>();
			Set<String> values = annotationsAndValues.get(annotation);
			if (values != null) {
				for (String value: values) classes.add(AnnotationService.getInstance().createValueClass(value));
			}
			AnnotationService.getInstance().createAnnotation(annotation, annotationsNumeric.get(annotation), pClass, classes);
		}
	}

}
