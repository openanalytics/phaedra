package eu.openanalytics.phaedra.ui.protocol.util;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.calculation.CalculationService.MultiploMethod;

public class MultiploMethodToStringConverter implements IConverter {

	@Override
	public Object getFromType() {
		return MultiploMethod.class;
	}
	
	@Override
	public Object getToType() {
		return String.class;
	}

	@Override
	public Object convert(Object fromObject) {
		MultiploMethod method = (MultiploMethod)fromObject;
		if (method == null || method == MultiploMethod.None) {
			return null;
		}
		return method.name();
	}

}
