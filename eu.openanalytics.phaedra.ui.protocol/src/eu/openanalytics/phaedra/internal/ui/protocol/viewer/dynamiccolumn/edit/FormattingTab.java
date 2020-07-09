package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit;

import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_DATA_TYPE_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_FORMAT_KEY;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.EditCustomColumnTab;
import eu.openanalytics.phaedra.base.ui.util.misc.OptionStack;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumnSupport;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueDataType;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueFormat;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueFormat.FormatEdit;


public class FormattingTab<TEntity> extends EditCustomColumnTab {
	
	
	private final DynamicColumnSupport<TEntity, ?> columnSupport;
	
	private final List<? extends ValueDataType> valueDataTypes;
	private final WritableValue<ValueDataType> valueDataTypeValue;
	
	private final Map<ValueDataType, ValueFormat> valueDataTypeFormatMap = new HashMap<>();
	private final WritableValue<ValueFormat> formatValue;
	private final Map<ValueFormat, FormatEdit> formatEditMap = new HashMap<>();
	private FormatEdit formatEdit;
	
	private ComboViewer dataTypeViewer;
	
	private ComboViewer formatViewer;
	
	private OptionStack formatDetail;
	
	
	public FormattingTab(final DynamicColumnSupport<TEntity, ?> columnSupport) {
		super("&Formatting");
		this.columnSupport = columnSupport;
		
		this.valueDataTypes = this.columnSupport.getValueDataTypes();
		this.valueDataTypeValue = new WritableValue<>(null, ValueDataType.class);
		this.formatValue = new WritableValue<>(null, ValueFormat.class);
	}
	
	
	protected FormatEdit getFormatEdit(final ValueFormat format) {
		if (format == null) {
			return null;
		}
		return formatEditMap.computeIfAbsent(format, (type) -> type.createEdit());
	}
	
	@Override
	protected Composite createContent(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(createContentGridLayout(3));
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Value &Type:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final ComboViewer viewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					if (element instanceof ValueDataType) {
						return ((ValueDataType)element).getLabel();
					}
					return super.getText(element);
				}
			});
			viewer.setInput(this.valueDataTypes);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			this.dataTypeViewer = viewer;
		}
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("For&mat:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final ComboViewer viewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					if (element instanceof ValueFormat) {
						return ((ValueFormat)element).getLabel();
					}
					return super.getText(element);
				}
			});
			viewer.setInput(Collections.EMPTY_LIST);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			this.formatViewer = viewer;
		}
		
		new Label(composite, SWT.NONE);
		this.formatDetail = new OptionStack(composite, SWT.NONE);
		this.formatDetail.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		return composite;
	}
	
	
	@Override
	protected void initDataBinding(final DataBindingContext dbc) {
		dbc.bindValue(
				ViewerProperties.singleSelection().observe(this.dataTypeViewer),
				this.valueDataTypeValue );
		
		IObservableValue<List<ValueFormat>> supportedFormats = PojoProperties.value(ValueDataType.class, "supportedFormats", List.class)
				.observeDetail(this.valueDataTypeValue);
		dbc.bindValue(
				ViewerProperties.input().observe(this.formatViewer),
				supportedFormats,
				new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
				new UpdateValueStrategy() );
		this.valueDataTypeValue.addValueChangeListener(new IValueChangeListener<ValueDataType>() {
			@Override
			public void handleValueChange(final ValueChangeEvent<? extends ValueDataType> event) {
				final ValueDataType oldType = event.diff.getOldValue();
				if (oldType != null) {
					valueDataTypeFormatMap.put(oldType, formatValue.getValue());
				}
				final ValueDataType newType = event.diff.getNewValue();
				if (newType != null) {
					formatValue.setValue(null); // required if old and new format are identical (e.g. default)
					formatViewer.setInput(newType.getSupportedFormats());
					formatViewer.getControl().setEnabled(newType.getSupportedFormats().size() > 1);
					ValueFormat format = valueDataTypeFormatMap.get(newType);
					if (format == null) {
						format = newType.getSupportedFormats().get(0);
					}
					formatValue.setValue(format);
				}
				else {
					formatViewer.setInput(Collections.EMPTY_LIST);
					formatViewer.getControl().setEnabled(false);
					formatValue.setValue(null);
				}
			}
		});
		dbc.bindValue(
				ViewerProperties.singleSelection().observe(this.formatViewer),
				this.formatValue );
		this.formatValue.addValueChangeListener(new IValueChangeListener<ValueFormat>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends ValueFormat> event) {
				formatEdit = getFormatEdit(event.diff.getNewValue());
				formatDetail.setActive(formatEdit);
			}
		});
		
		this.formatDetail.initDataBinding(dbc);
	}
	
	
	@Override
	protected void updateConfig(final Map<String, Object> customData) {
		final ValueDataType valueDataType = this.valueDataTypeValue.getValue();
		customData.put(VALUE_DATA_TYPE_KEY, valueDataType);
		
		final ValueFormat format = this.formatValue.getValue();
		customData.put(VALUE_FORMAT_KEY, format);
		formatEdit.updateConfig(customData);
	}
	
	@Override
	protected void updateTargets(final Map<String, Object> customData) {
		final ValueDataType valueDataType = (ValueDataType)customData.get(VALUE_DATA_TYPE_KEY);
		this.valueDataTypeValue.setValue(valueDataType);
		
		final ValueFormat format = (ValueFormat)customData.get(VALUE_FORMAT_KEY);
		this.formatValue.setValue(format);
		this.formatEdit.updateTargets(customData);
	}
	
}
