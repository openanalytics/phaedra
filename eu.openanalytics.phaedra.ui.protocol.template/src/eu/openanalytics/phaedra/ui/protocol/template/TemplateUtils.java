package eu.openanalytics.phaedra.ui.protocol.template;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.util.text.AnnotationStyle;
import eu.openanalytics.phaedra.base.ui.util.text.TextAnnotation;
import eu.openanalytics.phaedra.protocol.template.validation.ValidationItem;

public class TemplateUtils {

	public static TextAnnotation createAnnotation(ValidationItem problem) {
		if (problem.severity == ValidationItem.SEV_INFO) return AnnotationStyle.Info.create(problem.start, problem.end, problem.text);
		else if (problem.severity == ValidationItem.SEV_WARNING) return AnnotationStyle.Warning.create(problem.start, problem.end, problem.text);
		else if (problem.severity == ValidationItem.SEV_ERROR) return AnnotationStyle.Error.create(problem.start, problem.end, problem.text);
		else return new TextAnnotation(problem.start, problem.end, null, problem.text, null);
	}
	
	public static Image getImage(ValidationItem problem) {
		if (problem.severity == ValidationItem.SEV_INFO) return AnnotationStyle.Info.getImage();
		else if (problem.severity == ValidationItem.SEV_WARNING) return AnnotationStyle.Warning.getImage();
		else if (problem.severity == ValidationItem.SEV_ERROR) return AnnotationStyle.Error.getImage();
		else return null;
	}
}
