package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Map;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.OptionType;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueFormat.FormatConfig;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueFormat.FormatEdit;


public class ValueFormat extends OptionType<ValueFormat, FormatConfig, FormatEdit> {
	
	
	public class FormatConfig extends OptionType<ValueFormat, FormatConfig, FormatEdit>.OptionConfig {
		
		
		protected FormatConfig() {
		}
		
		
	}
	
	public class FormatEdit extends OptionType<ValueFormat, FormatConfig, FormatEdit>.OptionEdit {
		
		
		private String infoText;
		
		
		protected FormatEdit() {
		}
		
		protected FormatEdit(final String infoText) {
			this.infoText = infoText;
		}
		
		
		@Override
		protected Composite createControls(final Composite parent) {
			if (this.infoText != null) {
				final Composite composite = new Composite(parent, SWT.NONE);
				GridLayoutFactory.fillDefaults().applyTo(composite);
				
				final Label label = new Label(composite, SWT.WRAP);
				label.setText(this.infoText);
				label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
				
				return composite;
			}
			return null;
		}
		
	}
	
	
	public ValueFormat(final String key, final String label) {
		super(key, label);
	}
	
	
	@Override
	public Class<ValueFormat> getOptionType() {
		return ValueFormat.class;
	}
	
	@Override
	public FormatConfig createConfig(final Map<String, Object> customData) {
		return new FormatConfig();
	}
	
	public DataFormatter createDataFormatter(final FormatConfig formatConfig, final DataFormatter baseDataFormatter) {
		return baseDataFormatter;
	}
	
	@Override
	public FormatEdit createEdit() {
		return new FormatEdit();
	}
	
}
