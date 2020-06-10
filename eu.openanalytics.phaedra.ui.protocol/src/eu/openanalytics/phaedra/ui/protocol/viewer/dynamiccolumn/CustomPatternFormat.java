package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomDataUtils;


abstract class CustomPatternFormat extends ValueFormat {
	
	
	protected class PatternConfig extends FormatConfig {
		
		private final String pattern;
		
		
		public PatternConfig(final String pattern) {
			this.pattern = pattern;
		}
		
		public PatternConfig(final Map<String, Object> customData) {
			this.pattern = CustomDataUtils.getString(customData, CustomPatternFormat.this.patternKey, CustomPatternFormat.this.template);
		}
		
		
		public String getPattern() {
			return this.pattern;
		}
		
		@Override
		public void updateConfig(final Map<String, Object> customData) {
			super.updateConfig(customData);
			customData.put(CustomPatternFormat.this.patternKey, this.pattern);
		}
		
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (super.equals(obj)) {
				return this.pattern.equals(((PatternConfig)obj).pattern);
			}
			return false;
		}
		
	}
	
	protected class PatternEdit extends FormatEdit {
		
		
		private final WritableValue<String> patternValue;
		
		private Text patternControl;
		
		
		public PatternEdit() {
			this.patternValue = new WritableValue<>(null, String.class);
		}
		
		
		@Override
		protected Composite createControls(final Composite parent) {
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
			
			{	final Label label = new Label(composite, SWT.NONE);
				label.setText("Pattern:");
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
				final Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				this.patternControl = text;
			}
			
			return composite;
		}
		
		@Override
		protected void initDataBinding(final DataBindingContext dbc) {
			dbc.bindValue(
					WidgetProperties.text(SWT.Modify).observe(this.patternControl),
					this.patternValue,
					new UpdateValueStrategy().setAfterGetValidator(new IValidator() {
						@Override
						public IStatus validate(final Object value) {
							final String pattern = (String)value;
							if (pattern == null || pattern.trim().isEmpty()) {
								return ValidationStatus.error("Pattern for custom format is not specified.");
							}
							try {
								createFormatter((String)value);
								return ValidationStatus.ok();
							}
							catch (final IllegalArgumentException e) {
								return ValidationStatus.error("Pattern for custom format is invalid: " + e.getMessage());
							}
						}
					}),
					null );
			
			updateTargets(null);
		}
		
		@Override
		public PatternConfig getConfig() {
			if (getValidationStatus().getSeverity() >= IStatus.ERROR) {
				return null;
			}
			return new PatternConfig(this.patternValue.getValue());
		}
		
		@Override
		public void updateTargets(final Map<String, Object> customData) {
			final PatternConfig config = new PatternConfig(customData);
			this.patternValue.setValue(config.getPattern());
		}
		
	}
	
	
	private final String patternKey;
	
	private final String template;
	
	
	public CustomPatternFormat(final String key, final String label, final String template) {
		super(key, label);
		this.patternKey = createPropertyKey("Pattern");
		
		this.template = template;
	}
	
	
	protected abstract Object createFormatter(final String pattern) throws IllegalArgumentException;
	
	@Override
	public PatternConfig createConfig(final Map<String, Object> customData) {
		return new PatternConfig(customData);
	}
	
	@Override
	public FormatEdit createEdit() {
		return new PatternEdit();
	}
	
}
