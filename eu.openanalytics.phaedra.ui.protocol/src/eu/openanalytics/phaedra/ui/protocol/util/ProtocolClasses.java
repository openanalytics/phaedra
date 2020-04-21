package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;

import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public class ProtocolClasses<TEntity> extends WritableValue<Set<ProtocolClass>>
		implements IValueChangeListener<List<TEntity>> {
	
	
	private final Function<TEntity, ProtocolClass> getProtocolClass;
	
	
	public ProtocolClasses(final Function<TEntity, ProtocolClass> getProtocolClass) {
		this.getProtocolClass = getProtocolClass;
	}
	
	public ProtocolClasses(final IObservableValue<List<TEntity>> inputValue,
			final Function<TEntity, ProtocolClass> getProtocolClass) {
		this(getProtocolClass);
		inputValue.addValueChangeListener(this);
		final List<TEntity> baseElements = inputValue.getValue();
		if (baseElements != null) {
			setBaseElements(baseElements);
		}
	}
	
	
	@Override
	public void handleValueChange(final ValueChangeEvent<? extends List<TEntity>> event) {
		setBaseElements(event.diff.getNewValue());
	}
	
	private void setBaseElements(final List<TEntity> elements) {
		final Set<ProtocolClass> protocolClasses = new HashSet<>();
		for (final TEntity element : elements) {
			protocolClasses.add(getProtocolClass(element));
		}
		setValue(protocolClasses);
	}
	
	public final ProtocolClass getProtocolClass(final TEntity element) {
		return this.getProtocolClass.apply(element);
	}
	
	
	@Override
	public void doSetValue(final Set<ProtocolClass> value) {
		if (!value.equals(doGetValue())) {
			super.doSetValue(value);
		}
	}
	
}
