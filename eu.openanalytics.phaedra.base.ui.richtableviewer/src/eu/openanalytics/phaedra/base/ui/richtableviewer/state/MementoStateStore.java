package eu.openanalytics.phaedra.base.ui.richtableviewer.state;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;


/**
 * Abstract StateStore implementation using {@link IMemento} to encode/decode the state to/from
 * XML string.
 */
public abstract class MementoStateStore implements IStateStore {
	
	private static final String TAG_ROOT = "column-config";
	private static final String TAG_COLUMN = "column";
	private static final String TAG_CUSTOM_DATA = "customData";
	private static final String TAG_ENTRY = "entry";
	
	private static final String ATTR_KEY = "key";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_DESCRIPTION = "description";
	private static final String ATTR_WIDTH = "width";
	private static final String ATTR_HIDDEN = "hidden";
	private static final String ATTR_SORT = "sort";
	private static final String ATTR_VALUE = "value";
	
	private final Map<String, ListenerList<Consumer<StateChangedEvent>>> listeners = new ConcurrentHashMap<>();
	
	private final ExecutorService backgroundSaver;
	
	
	public MementoStateStore() {
		this.backgroundSaver = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public ColumnConfiguration[] loadState(final String tableKey) throws IOException {
		final String stateString = loadStateString(tableKey);
		if (stateString == null) {
			return null;
		}
		return decode(stateString);
	}
	
	@Override
	public void saveState(final ColumnConfiguration[] configs, final String tableKey,
			final Object source, final int notification) throws IOException {
		if (configs == null || configs.length == 0) {
			return;
		}
		final String stateString = encode(configs);
		this.backgroundSaver.submit(() -> {
			if (saveStateString(tableKey, stateString) && notification != 0) {
				final ListenerList<Consumer<StateChangedEvent>> listenerList = this.listeners.get(tableKey);
				if (listenerList != null && !listenerList.isEmpty()) {
					try {
						final ColumnConfiguration[] state = decode(stateString);
						if (state != null) {
							final StateChangedEvent event = new StateChangedEvent(source, Arrays.asList(configs), notification);
							Display.getDefault().asyncExec(() -> notifyListener(listenerList, event));
						}
					}
					catch (final Exception e) {
						EclipseLog.error("Failed to load column state", e, Activator.getDefault());
					}
				}
			}
		});
	}
	
	@Override
	public void addListener(final String tableKey, final Consumer<StateChangedEvent> listener) {
		ListenerList<Consumer<StateChangedEvent>> listenerList = this.listeners.get(tableKey);
		if (listenerList == null) {
			listenerList = new ListenerList<>(ListenerList.IDENTITY);
			this.listeners.put(tableKey, listenerList);
		}
		listenerList.add(listener);
	}
	
	@Override
	public void removeListener(final String tableKey, final Consumer<StateChangedEvent> listener) {
		final ListenerList<Consumer<StateChangedEvent>> listenerList = this.listeners.get(tableKey);
		if (listenerList == null) {
			return;
		}
		listenerList.remove(listener);
	}
	
	
	protected abstract String loadStateString(final String tableKey) throws IOException;
	
	protected abstract boolean saveStateString(final String tableKey, final String value);
	
	
	private void notifyListener(final ListenerList<Consumer<StateChangedEvent>> listenerList, final StateChangedEvent event) {
		for (final Consumer<StateChangedEvent> listener : listenerList) {
			listener.accept(event);
		}
	}
	
	
	protected ColumnConfiguration[] decode(final String stateSource) throws IOException {
		final StringReader reader = new StringReader(stateSource);
		return load(reader);
	}
	
	protected String encode(final ColumnConfiguration[] columnConfigs) throws IOException {
		final StringWriter writer = new StringWriter();
		save(columnConfigs, writer);
		return writer.getBuffer().toString();
	}
	
	
	private static ColumnConfiguration[] load(final Reader stateSource) throws IOException {
		try {
			final XMLMemento memento = XMLMemento.createReadRoot(stateSource);
			final IMemento[] children = memento.getChildren(TAG_COLUMN);
			
			final List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
			for (int i = 0; i < children.length; i++) {
				final ColumnConfiguration config = loadColumn(children[i]);
				configs.add(config);
			}
			return configs.toArray(new ColumnConfiguration[configs.size()]);
		} catch (final WorkbenchException e) {
			throw new IOException("Failed to parse memento", e);
		}
	}
	
	private static void save(final ColumnConfiguration[] configs, final Writer writer) throws IOException {
		final XMLMemento memento = XMLMemento.createWriteRoot(TAG_ROOT);
		for (final ColumnConfiguration config : configs) {
			saveColumn(config, memento.createChild(TAG_COLUMN));
		}
		memento.save(writer);
	}
	
	private static ColumnConfiguration loadColumn(final IMemento memento) {
		final ColumnConfiguration config = new ColumnConfiguration();
		config.setKey(memento.getString(ATTR_KEY));
		if (config.isCustom()) {
			config.setName(memento.getString(ATTR_NAME));
			config.setTooltip(memento.getString(ATTR_DESCRIPTION));
		}
		config.setCustomData(loadCustomData(memento.getChild(TAG_CUSTOM_DATA)));
		final Integer width = memento.getInteger(ATTR_WIDTH);
		if (width != null) {
			config.setWidth(width);
		}
		final Boolean hidden = memento.getBoolean(ATTR_HIDDEN);
		if (hidden != null) {
			config.setHidden(hidden);
		}
		final Integer sort = memento.getInteger(ATTR_SORT);
		if (sort != null) {
			config.setSortDirection(sort);
		}
		return config;
	}
	
	private static void saveColumn(final ColumnConfiguration config, final IMemento memento) {
		memento.putString(ATTR_KEY, config.getKey());
		if (config.isCustom()) {
			memento.putString(ATTR_NAME, config.getName());
			memento.putString(ATTR_DESCRIPTION, config.getTooltip());
		}
		final Map<String, Object> customData = config.getCustomData();
		if (customData != null) {
			saveCustomData(customData, memento.createChild(TAG_CUSTOM_DATA));
		}
		memento.putInteger(ATTR_WIDTH, config.getWidth());
		memento.putBoolean(ATTR_HIDDEN, config.isHidden());
		memento.putInteger(ATTR_SORT, config.getSortDirection());
	}
	
	private static Map<String, Object> loadCustomData(final IMemento memento) {
		if (memento == null) {
			return null;
		}
		final Map<String, Object> customData = new HashMap<>();
		for (final IMemento entryMemento : memento.getChildren()) {
			if (entryMemento.getType().equals(TAG_ENTRY)) {
				final String key = entryMemento.getString(ATTR_KEY);
				if (key != null) {
					customData.put(key, entryMemento.getString(ATTR_VALUE));
				}
			}
		}
		return customData;
	}
	
	private static void saveCustomData(final Map<String, Object> customData, final IMemento memento) {
		for (final Map.Entry<String, Object> entry : customData.entrySet()) {
			final String key = entry.getKey();
			if (key == null || key.isEmpty() || key.charAt(0) == '.') {
				continue;
			}
			final IMemento entryMemento = memento.createChild(TAG_ENTRY);
			entryMemento.putString(ATTR_KEY, key);
			final Object value = entry.getValue();
			if (value != null) {
				entryMemento.putString(ATTR_VALUE, value.toString());
			}
		}
	}
	
}
