package eu.openanalytics.phaedra.ui.user;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.state.IStateStore;
import eu.openanalytics.phaedra.model.user.UserService;

public class MementoStateStore implements IStateStore {

	private final static String TAG_ROOT = "column-config";
	private final static String TAG_COLUMN = "column";
	private final static String ATTR_KEY = "key";
	private final static String ATTR_WIDTH = "width";
	private final static String ATTR_HIDDEN = "hidden";
	private final static String ATTR_SORT = "sort";
	
	private ExecutorService backgroundSaver;
	
	public MementoStateStore() {
		backgroundSaver = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public ColumnConfiguration[] loadState(String tableKey) throws IOException {
		String value = UserService.getInstance().getPreferenceValue("Memento", tableKey);
		if (value == null) return null;
		return load(new StringReader(value));
	}
	
	@Override
	public void saveState(ColumnConfiguration[] configs, String tableKey) throws IOException {
		if (configs == null || configs.length == 0) return;
		StringWriter writer = new StringWriter();
		save(configs, writer);
		String value = writer.getBuffer().toString();
		backgroundSaver.submit(() -> {
			UserService.getInstance().setPreferenceValue("Memento", tableKey, value);
		});
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private static ColumnConfiguration[] load(Reader stateSource) throws IOException {
		try {
			XMLMemento memento = XMLMemento.createReadRoot(stateSource);
			IMemento[] children = memento.getChildren(TAG_COLUMN);

			List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
			for (int i = 0; i < children.length; i++) {
				ColumnConfiguration config = loadMemento(children[i]);
				configs.add(config);
			}
			return configs.toArray(new ColumnConfiguration[configs.size()]);
		} catch (WorkbenchException e) {
			throw new IOException("Failed to parse memento", e);
		}
	}

	private static void save(ColumnConfiguration[] configs, Writer writer) throws IOException {
		XMLMemento memento = createMemento(configs);
		memento.save(writer);
	}

	private static ColumnConfiguration loadMemento(IMemento memento) {
		ColumnConfiguration config = new ColumnConfiguration();
		config.setKey(memento.getString(ATTR_KEY));
		Integer width = memento.getInteger(ATTR_WIDTH);
		if (width != null) config.setWidth(width);
		Boolean hidden = memento.getBoolean(ATTR_HIDDEN);
		if (hidden != null) config.setHidden(hidden);
		Integer sort = memento.getInteger(ATTR_SORT);
		if (sort != null) config.setSortDirection(sort);
		return config;
	}
	
	private static XMLMemento createMemento(ColumnConfiguration[] configs) {
		
		XMLMemento memento = XMLMemento.createWriteRoot(TAG_ROOT);

		for (ColumnConfiguration config: configs) {
			IMemento column = memento.createChild(TAG_COLUMN);
			column.putString(ATTR_KEY, config.getKey());
			column.putInteger(ATTR_WIDTH, config.getWidth());
			column.putBoolean(ATTR_HIDDEN, config.isHidden());
			column.putInteger(ATTR_SORT, config.getSortDirection());
		}
		
		return memento;
	}
}
