package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.OptionType.OptionConfig;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.OptionType.OptionEdit;
import eu.openanalytics.phaedra.base.ui.util.misc.OptionStack;


public abstract class OptionType<TType extends OptionType, TConfig extends OptionConfig, TEdit extends OptionEdit> {
	
	
	public class OptionConfig {
		
		
		protected OptionConfig() {
		}
		
		
		public TType getType() {
			return (TType)OptionType.this;
		}
		
		
		public void updateConfig(final Map<String, Object> customData) {
		}
		
		
		@Override
		public int hashCode() {
			return OptionType.this.hashCode();
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof OptionType.OptionConfig && getClass() == obj.getClass()) {
				return getType().equals(((OptionType.OptionConfig)obj).getType());
			}
			return false;
		}
		
	}
	
	public class OptionEdit extends OptionStack.Option {
		
		
		protected OptionEdit() {
		}
		
		
		public TType getType() {
			return (TType)OptionType.this;
		}
		
		public TConfig getConfig() {
			return null;
		}
		
		@Override
		protected void initInput() {
			updateTargets(null);
		}
		
		public void updateConfig(final Map<String, Object> customData) {
			final TConfig config = getConfig();
			if (config != null) {
				config.updateConfig(customData);
			}
		}
		
		public void updateTargets(final Map<String, Object> customData) {
		}
		
	}
	
	
	private String key;
	
	private final String label;
	
	
	public OptionType(final String key, final String label) {
		this.key = key;
		this.label = label;
	}
	
	
	public abstract Class<TType> getOptionType();
	
	public String getKey() {
		return this.key;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	protected String createPropertyKey(final String property) {
		return getOptionType().getSimpleName() + '.' + this.key + '.' + property;
	}
	
	public abstract TConfig createConfig(final Map<String, Object> customConfig);
	
	public abstract TEdit createEdit();
	
	
	@Override
	public String toString() {
		return this.key;
	}
	
	
	public static <T extends OptionType<?, ?, ?>> T getType(final List<? extends T> list, final String key) {
		if (key != null) {
			for (final T type : list) {
				if (type.getKey().equals(key)) {
					return type;
				}
			}
		}
		return null;
	}
	
}
