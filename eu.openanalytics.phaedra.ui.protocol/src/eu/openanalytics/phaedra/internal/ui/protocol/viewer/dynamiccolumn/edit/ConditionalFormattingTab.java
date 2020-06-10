package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit;

import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.CONDITIONAL_FORMAT_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.EditCustomColumnTab;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.OptionType;
import eu.openanalytics.phaedra.base.ui.util.misc.OptionStack;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.Renderer;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumnSupport;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumns;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat.FormatConfig;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat.FormatEdit;


public class ConditionalFormattingTab<TEntity> extends EditCustomColumnTab {
	
	
	private final DynamicColumnSupport<TEntity, ?> columnSupport;
	
	private List<? extends ConditionalFormat> availableFormats;
	
	private final WritableValue<ConditionalFormat> formatValue;
	private final Map<ConditionalFormat, FormatEdit> formatEditMap = new HashMap<>();
	private FormatEdit formatEdit;
	
	private ComboViewer formatViewer;
	
	private OptionStack formatDetail;
	
	private Composite previewControl;
	private Renderer previewRenderer;
	
	
	public ConditionalFormattingTab(final DynamicColumnSupport<TEntity, ?> columnSupport) {
		super("Con&ditional Formatting");
		this.columnSupport = columnSupport;
		
		this.availableFormats = this.columnSupport.getConditionalFormats();
		this.formatValue = new WritableValue<>(null, ConditionalFormat.class);
	}
	
	
	protected FormatEdit getFormatEdit(final ConditionalFormat format) {
		if (format == null) {
			return null;
		}
		return formatEditMap.computeIfAbsent(format, (type) -> type.createEdit());
	}
	
	@Override
	protected Composite createContent(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(createContentGridLayout(2));

		{	final ComboViewer viewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					if (element instanceof OptionType) {
						return ((OptionType)element).getLabel();
					}
					return super.getText(element);
				}
			});
			viewer.setInput(this.availableFormats);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			this.formatViewer = viewer;
		}
		
		this.formatDetail = new OptionStack(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.indent(getDetailHorizontalIndent(), 0)
				.span(2, 1).grab(true, true)
				.applyTo(this.formatDetail);
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Preview:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final Composite preview = new Composite(composite, SWT.NONE);
			preview.addListener(SWT.Paint, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					final Rectangle previewBounds = preview.getClientArea();
					
					final GC gc = event.gc;
					Color oldForeground = gc.getForeground();
					Color oldBackground = gc.getBackground();
					
					final int[] values = { 0, 25, 50, 75, 100 };
					int gap = 6;
					int cellWidth = (previewBounds.width + gap - 1) / values.length;
					
					for (int i = 0; i < values.length; i++) {
						int x = previewBounds.x + i * cellWidth;
						gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
						gc.fillRectangle(x, previewBounds.y, cellWidth - gap, previewBounds.height);
						final Renderer renderer = previewRenderer;
						if (renderer != null) {
							renderer.paint(gc,
									new Rectangle(x + 1, previewBounds.y + 1, cellWidth - gap - 1, previewBounds.height - 2),
									String.format("%1$s%%", values[i]), (double)values[i]/100 );
						}
						gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
						gc.setLineWidth(1);
						gc.drawRectangle(x, previewBounds.y, cellWidth - gap, previewBounds.height - 1);
					}
					
					gc.setForeground(oldForeground);
					gc.setBackground(oldBackground);
				}
			});
			final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
//			gridData.widthHint = 240;
			gridData.heightHint = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 4;
			preview.setLayoutData(gridData);
			this.previewControl = preview;
		}
		
		return composite;
	}
	
	protected void updatePreview() {
		if (this.previewControl == null) {
			return;
		}
		Renderer renderer = this.previewRenderer;
		if (renderer != null) {
			renderer.dispose();
		}
		renderer = null;
		final FormatEdit edit = this.formatEdit;
		if (edit != null) {
			final FormatConfig config = edit.getConfig();
			if (config != null) {
				renderer = config.getType().createRenderer(config);
			}
		}
		this.previewRenderer = renderer;
		
		this.previewControl.redraw();
	}
	
	@Override
	protected void initDataBinding(final DataBindingContext dbc) {
		dbc.bindValue(
				ViewersObservables.observeSingleSelection(this.formatViewer),
				this.formatValue );
		this.formatValue.addValueChangeListener(new IValueChangeListener<ConditionalFormat>() {
			@Override
			public void handleValueChange(final ValueChangeEvent<? extends ConditionalFormat> event) {
				final ConditionalFormat newFormat = event.diff.getNewValue();
				formatEdit = getFormatEdit(newFormat);
				formatDetail.setActive(formatEdit);
				updatePreview();
			}
		});
		
		this.formatDetail.initDataBinding(dbc, new IChangeListener() {
			@Override
			public void handleChange(final ChangeEvent event) {
				updatePreview();
			}
		});
	}
	
	@Override
	protected void updateConfig(final Map<String, Object> customData) {
		final ConditionalFormat format = this.formatValue.getValue();
		if (format != DynamicColumns.NONE_CONDITIONAL_FORMAT) {
			customData.put(CONDITIONAL_FORMAT_KEY, format);
		}
		this.formatEdit.updateConfig(customData);
	}
	
	@Override
	protected void updateTargets(final Map<String, Object> customData) {
		ConditionalFormat format = (ConditionalFormat)customData.get(CONDITIONAL_FORMAT_KEY);
		if (format == null) {
			format = DynamicColumns.NONE_CONDITIONAL_FORMAT;
		}
		this.formatValue.setValue(format);
		this.formatEdit.updateTargets(customData);
	}
	
}
