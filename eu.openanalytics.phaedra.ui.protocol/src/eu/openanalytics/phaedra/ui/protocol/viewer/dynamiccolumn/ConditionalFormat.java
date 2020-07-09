package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Map;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomDataUtils;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.OptionType;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.Renderer;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat.FormatConfig;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat.FormatEdit;


public class ConditionalFormat extends OptionType<ConditionalFormat, FormatConfig, FormatEdit> {
	
	
	public class FormatConfig extends OptionType<ConditionalFormat, FormatConfig, FormatEdit>.OptionConfig {
		
		
		private final double min;
		private final double max;
		
		
		protected FormatConfig(final double min, final double max) {
			this.min = min;
			this.max = max;
		}
		
		protected FormatConfig(final Map<String, Object> customData) {
			this.min = CustomDataUtils.getDoubleValue(customData, ConditionalFormat.this.minKey, 0);
			this.max= CustomDataUtils.getDoubleValue(customData, ConditionalFormat.this.maxKey, 1);
		}
		
		
		public double getMin() {
			return this.min;
		}
		
		public double getMax() {
			return this.max;
		}
		
		@Override
		public void updateConfig(final Map<String, Object> customData) {
			customData.put(ConditionalFormat.this.minKey, this.min);
			customData.put(ConditionalFormat.this.maxKey, this.max);
		}
		
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (super.equals(obj)) {
				final FormatConfig other = (FormatConfig)obj;
				return (this.min == other.min && this.max == other.max);
			}
			return false;
		}
		
	}
	
	public class FormatEdit extends OptionType<ConditionalFormat, FormatConfig, FormatEdit>.OptionEdit {
		
		
		protected FormatEdit() {
		}
		
	}
	
	
	private final String minKey;
	private final String maxKey;
	
	
	public ConditionalFormat(final String key, final String label) {
		super(key, label);
		this.minKey = createPropertyKey("Min");
		this.maxKey = createPropertyKey("Max");
	}
	
	
	@Override
	public Class<ConditionalFormat> getOptionType() {
		return ConditionalFormat.class;
	}
	
	@Override
	public FormatConfig createConfig(final Map<String, Object> customData) {
		return new FormatConfig(customData);
	}
	
	public Renderer createRenderer(final FormatConfig formatConfig) {
		return null;
	}
	
	@Override
	public FormatEdit createEdit() {
		return new FormatEdit();
	}
	
}
