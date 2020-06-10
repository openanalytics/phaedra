package eu.openanalytics.phaedra.base.ui.richtableviewer.state;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;


public interface IStateStore {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".stateStore";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	
	class StateChangedEvent {
		
		private final Object source;
		private final List<ColumnConfiguration> configs;
		private final int detail;
		
		
		public StateChangedEvent(final Object source, final List<ColumnConfiguration> configs, final int detail) {
			this.source= source;
			this.configs= configs;
			this.detail = detail;
		}
		
		
		public Object getSource() {
			return this.source;
		}
		
		public List<ColumnConfiguration> getConfigs() {
			return this.configs;
		}
		
		public int getDetail() {
			return this.detail;
		}
		
	}
	
	
	void addListener(final String tableKey, final Consumer<StateChangedEvent> listener);
	void removeListener(final String tableKey, final Consumer<StateChangedEvent> listener);
	
	
	public ColumnConfiguration[] loadState(String tableKey) throws IOException;
	
	public void saveState(ColumnConfiguration[] configs, String tableKey,
			final Object source, final int notification) throws IOException;
	
}
