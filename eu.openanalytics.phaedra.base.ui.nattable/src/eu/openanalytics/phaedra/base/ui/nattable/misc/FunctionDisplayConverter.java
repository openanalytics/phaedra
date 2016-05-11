package eu.openanalytics.phaedra.base.ui.nattable.misc;

import java.util.function.Function;

import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;

public class FunctionDisplayConverter extends DefaultDisplayConverter {

	private Function<Object, Object> canonicalToDisplay;
	private Function<Object, Object> displayToCanonical;

	public FunctionDisplayConverter(Function<Object, Object> canonicalToDisplay) {
		this.canonicalToDisplay = canonicalToDisplay;
		this.displayToCanonical = o -> o;
	}

	public FunctionDisplayConverter(Function<Object, Object> canonicalToDisplay, Function<Object, Object> displayToCanonical) {
		this.canonicalToDisplay = canonicalToDisplay;
		this.displayToCanonical = displayToCanonical;
	}

	@Override
	public Object canonicalToDisplayValue(Object canonicalValue) {
		return canonicalToDisplay.apply(canonicalValue);
	}

	@Override
	public Object displayToCanonicalValue(Object displayValue) {
		return displayToCanonical.apply(displayValue);
	}

}
