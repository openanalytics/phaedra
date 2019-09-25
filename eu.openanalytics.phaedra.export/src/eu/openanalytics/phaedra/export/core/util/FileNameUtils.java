package eu.openanalytics.phaedra.export.core.util;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class FileNameUtils {

	public final static char ESCAPE_CHAR = '_';
	
	public static String proposeName(IValueObject vo, String suffix) {
		if (vo == null) return "Export_" + suffix;
		Experiment exp = SelectionUtils.getAsClass(vo, Experiment.class);
		return exp.getName() + "_" + suffix;
	}
	
	public static String escape(String fileName) {
		String escaped = fileName.replace('/',ESCAPE_CHAR);
		escaped = escaped.replace('?',ESCAPE_CHAR);
		escaped = escaped.replace('<',ESCAPE_CHAR);
		escaped = escaped.replace('>',ESCAPE_CHAR);
		escaped = escaped.replace('\\',ESCAPE_CHAR);
		escaped = escaped.replace(':',ESCAPE_CHAR);
		escaped = escaped.replace('*',ESCAPE_CHAR);
		escaped = escaped.replace('|',ESCAPE_CHAR);
		return escaped;
	}
}
