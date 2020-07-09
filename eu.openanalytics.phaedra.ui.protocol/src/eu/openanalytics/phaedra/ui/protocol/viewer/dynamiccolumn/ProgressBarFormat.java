package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomDataUtils;
import eu.openanalytics.phaedra.base.ui.theme.ColorDefinition;
import eu.openanalytics.phaedra.base.ui.theme.PhaedraThemes;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.ProgressBarRenderer;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.Renderer;;


public class ProgressBarFormat extends ConditionalFormat {
	
	
	public class ProgressBarConfig extends FormatConfig {
		
		
		private final String colorId;
		
		
		public ProgressBarConfig(final double min, final double max,
				final String color) {
			super(min, max);
			this.colorId = color;
		}
		
		public ProgressBarConfig(final Map<String, Object> customData) {
			super(customData);
			this.colorId = CustomDataUtils.getString(customData, ProgressBarFormat.this.colorKey, PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR_ID);
		}
		
		
		public String getColorId() {
			return this.colorId;
		}
		
		
		@Override
		public void updateConfig(final Map<String, Object> customData) {
			super.updateConfig(customData);
			customData.put(ProgressBarFormat.this.colorKey, this.colorId);
		}
		
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (super.equals(obj)) {
				final ProgressBarConfig other = (ProgressBarConfig)obj;
				return (this.colorId.equals(other.colorId));
			}
			return false;
		}
		
	}
	
	
	private class ProgressBarEdit extends FormatEdit {
		
		
		private final List<ColorDefinition> predefinedColors = Arrays.asList(
				PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR,
				PhaedraThemes.RED_BACKGROUND_INDICATOR_COLOR );
		
		private final WritableValue<Double> minValue;
		private final WritableValue<Double> maxValue;
		
		private final WritableValue<ColorDefinition> colorValue;
		
		private Text minControl;
		private Text maxControl;
		
		private ComboViewer colorViewer;
		
		
		protected ProgressBarEdit() {
			this.minValue = new WritableValue<>(0.0, Double.class);
			this.maxValue = new WritableValue<>(0.0, Double.class);
			this.colorValue = new WritableValue<>(null, ColorDefinition.class);
		}
		
		
		@Override
		protected Composite createControls(final Composite parent) {
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(composite);
			
			{	final Label label = new Label(composite, SWT.NONE);
				label.setText("Min-Max:");
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
				final GridDataFactory gdFactory = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
						.hint(50, SWT.DEFAULT);
				final Text min = new Text(composite, SWT.SINGLE | SWT.BORDER);
				gdFactory.applyTo(min);
				this.minControl = min;
				final Text max = new Text(composite, SWT.SINGLE | SWT.BORDER);
				gdFactory.applyTo(max);
				this.maxControl = max;
			}
			new Label(composite, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			{	final Label label = new Label(composite, SWT.NONE);
				label.setText("Color:");
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
				final ComboViewer viewer = new ComboViewer(new CCombo(composite, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER));
				viewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(final Object element) {
						return ((ColorDefinition)element).getName();
					}
				});
				viewer.setContentProvider(new ArrayContentProvider());
				viewer.setInput(this.predefinedColors);
				GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1)
						.hint(80, SWT.DEFAULT)
						.applyTo(viewer.getControl());
				this.colorViewer = viewer;
			}
			
			return composite;
		}
		
		@Override
		protected void initDataBinding(final DataBindingContext dbc) {
			dbc.bindValue(
					WidgetProperties.text(SWT.Modify).observe(this.minControl),
					this.minValue );
			dbc.bindValue(
					WidgetProperties.text(SWT.Modify).observe(this.maxControl),
					this.maxValue );
			dbc.bindValue(
					ViewersObservables.observeSingleSelection(this.colorViewer),
					this.colorValue );
		}
		
		@Override
		public ProgressBarConfig getConfig() {
			if (getValidationStatus().getSeverity() >= IStatus.ERROR) {
				return null;
			}
			return new ProgressBarConfig(
					this.minValue.getValue(), this.maxValue.getValue(),
					this.colorValue.getValue().getId() );
		}
		
		@Override
		public void updateTargets(final Map<String, Object> customData) {
			final ProgressBarConfig config = new ProgressBarConfig(customData);
			this.minValue.setValue(config.getMin());
			this.maxValue.setValue(config.getMax());
			ColorDefinition definition = PhaedraThemes.getDefinition(this.predefinedColors, config.getColorId());
			if (definition == null) {
				definition = this.predefinedColors.get(0);
			}
			this.colorValue.setValue(definition);
		}
		
	}
	
	
	private final String colorKey;
	
	
	public ProgressBarFormat() {
		super("ProgressBar", "Progress Bar");
		this.colorKey = createPropertyKey("Color");
	}
	
	
	@Override
	public ProgressBarConfig createConfig(final Map<String, Object> customData) {
		return new ProgressBarConfig(customData);
	}
	
	@Override
	public Renderer createRenderer(final FormatConfig formatConfig) {
		final ProgressBarConfig config = (ProgressBarConfig)formatConfig;
		return new ProgressBarRenderer(PhaedraThemes.getColorRegistry().get(config.getColorId()));
	}
	
	@Override
	public FormatEdit createEdit() {
		return new ProgressBarEdit();
	}
	
}
