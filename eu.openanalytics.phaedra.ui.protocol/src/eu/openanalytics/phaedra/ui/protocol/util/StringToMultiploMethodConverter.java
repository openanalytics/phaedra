package eu.openanalytics.phaedra.ui.protocol.util;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.calculation.CalculationService.MultiploMethod;

public class StringToMultiploMethodConverter implements IConverter {

	@Override
	public Object getFromType() {
		return String.class;
	}
	
	@Override
	public Object getToType() {
		return MultiploMethod.class;
	}

	@Override
	public Object convert(Object fromObject) {
		String name = (String)fromObject;
		if (name == null) {
			return MultiploMethod.None;
		}
		return MultiploMethod.get(name);
	}

}
