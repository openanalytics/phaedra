package eu.openanalytics.phaedra.base.ui.util.tooltip;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

public abstract class AdvancedToolTip extends DefaultToolTip {

	private DataConverter dataConverter;

	private ToolTipLabelProvider labelProvider;

	public AdvancedToolTip(Control control, int style, boolean manualActivation) {
		super(control, style, manualActivation);

		control.addListener(SWT.Dispose, e-> dispose());
	}

	public void setLabelProvider(ToolTipLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	/**
	 * Add an optional data converter.
	 * @param dataConverter
	 */
	public void setDataConverter(DataConverter dataConverter) {
		this.dataConverter = dataConverter;
	}

	/**
	 * Should return the data over which the mouse is hovering.
	 * This data will be send to to the DataConverter if one is present.
	 *
	 * @param event
	 * @return The data over which the mouse is hovering.
	 */
	public abstract Object getData(Event event);

	public void dispose() {
		labelProvider.dispose();
	}

	/**
	 * Creates the content are of the the tooltip. By default this creates a
	 * CLabel to display text. To customize the text Subclasses may override the
	 * following methods
	 * <ul>
	 * <li>{@link #getStyle(Event)}</li>
	 * <li>{@link #getBackgroundColor(Event)}</li>
	 * <li>{@link #getForegroundColor(Event)}</li>
	 * <li>{@link #getFont(Event)}</li>
	 * <li>{@link #getImage(Event)}</li>
	 * <li>{@link #getText(Event)}</li>
	 * <li>{@link #getBackgroundImage(Event)}</li>
	 * </ul>
	 *
	 * @param event
	 *            the event that triggered the activation of the tooltip
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		Image image = getImage(event);
		Image bgImage = getBackgroundImage(event);
		String text = getText(event);
		Color fgColor = getForegroundColor(event);
		Color bgColor = getBackgroundColor(event);
		Font font = getFont(event);

		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(comp);

		Composite topArea = new Composite(comp, SWT.NONE);
		if (bgColor != null) topArea.setBackground(bgColor);
		if (bgImage != null) topArea.setBackgroundImage(bgImage);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(topArea);
		GridLayoutFactory.fillDefaults().spacing(1, 1).extendedMargins(2, 2, 2, 2).applyTo(topArea);

		Label l = null;
		if (image != null) {
			l = new Label(topArea, SWT.NONE);
			l.setImage(image);
			if (bgColor != null) l.setBackground(bgColor);
			if (fgColor != null) l.setForeground(fgColor);
		}
		final Label labelImg = l;

		if (text != null && !text.isEmpty()) {
			l = new Label(topArea, SWT.NONE);
			l.setText(text);
			if (font != null) l.setFont(font);
			if (bgColor != null) l.setBackground(bgColor);
			if (fgColor != null) l.setForeground(fgColor);
		}
		final Label labelText = l;

		IToolTipUpdate update = () -> {
			labelImg.setImage(getImage(event));
			labelText.setText(getText(event));
		};

		if (labelProvider.hasAdvancedControls()) {
			labelProvider.fillAdvancedControls(topArea, convertData(getData(event)), update);
		}

		return comp;
	}

	@Override
	public boolean isHideOnMouseDown() {
		return labelProvider.isHideOnMouseDown();
	}

	@Override
	protected final Image getImage(Event event) {
		Object data = convertData(getData(event));
		if (data == null) return null;
		return labelProvider.getImage(data);
	}

	@Override
	protected final String getText(Event event) {
		Object data = convertData(getData(event));
		if (data == null) return null;
		return labelProvider.getText(data);
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		return super.shouldCreateToolTip(event) && (getText(event) != null || getImage(event) != null);
	}

	@Override
	protected Object getToolTipArea(Event event) {
		return getData(event);
	}

	private Object convertData(Object data) {
		if (data != null && dataConverter != null) {
			return dataConverter.convert(data);
		}
		return data;
	}

}
