package eu.openanalytics.phaedra.base.datatype.util;

import java.util.function.Supplier;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;


public abstract class DataConfigSupport<T extends DataUnitConfig> implements Supplier<T> {
	
	
	private T config;
	
	private IPropertyChangeListener preferenceListener;
	
	private final ListenerList<Runnable> listeners = new ListenerList<>(ListenerList.IDENTITY);
	private final Runnable finalListener;
	
	
	public DataConfigSupport(final Runnable onConfigChangedRunnable) {
		this.finalListener = onConfigChangedRunnable;
		initPreferenceListener();
	}
	
	private void initPreferenceListener() {
		this.preferenceListener = this::onPreferenceChanged;
		DataTypePrefs.getPreferenceStore().addPropertyChangeListener(this.preferenceListener);
		this.config = createConfig();
	}
	
	public void dispose() {
		if (this.preferenceListener != null) {
			DataTypePrefs.getPreferenceStore().removePropertyChangeListener(this.preferenceListener);
			this.preferenceListener = null;
		}
	}
	
	
	public void addListener(final Runnable listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(final Runnable listener) {
		this.listeners.remove(listener);
	}
	
	
	protected abstract T createConfig();
	
	
	protected void onPreferenceChanged(final PropertyChangeEvent event) {
		this.config = createConfig();
		onConfigChanged();
	}
	
	protected void onConfigChanged() {
		for (final Runnable listener : this.listeners) {
			listener.run();
		}
		if (this.finalListener != null) {
			this.finalListener.run();
		}
	}
	
	@Override
	public final T get() {
		return this.config;
	}
	
}
