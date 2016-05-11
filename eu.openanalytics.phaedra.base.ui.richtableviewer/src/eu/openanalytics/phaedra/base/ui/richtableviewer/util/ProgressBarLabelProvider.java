package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;

public class ProgressBarLabelProvider extends OwnerDrawLabelProvider {

	private ColumnConfiguration config;

	private Color foreground;
	private Color background;

	public ProgressBarLabelProvider(ColumnConfiguration config) {
		this.config = config;
	}

	public ProgressBarLabelProvider(ColumnConfiguration config, Color foreground, Color background) {
		this.config = config;
		this.foreground = foreground;
		this.background = background;
	}

	public void setForeground(Color foreground) {
		this.foreground = foreground;
	}

	public void setBackground(Color background) {
		this.background = background;
	}

	@Override
	public void dispose() {
		if (foreground != null) foreground.dispose();
		if (background != null) background.dispose();
		super.dispose();
	}

	@Override
	protected void measure(Event event, Object element) {
		int width = config.getWidth();
		event.setBounds(new Rectangle(event.x, event.y, width, event.height));
	}

	@Override
	protected void paint(Event event, Object element) {
		GC gc = event.gc;

		int width = config.getWidth();
		double percentage = getPercentage(element);
		if (percentage < 0) percentage = 0;
		if (percentage > 1) percentage = 1;

		// Background: progress bar
		int fillWidth = (int) (width * percentage);
		if (background == null) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_CYAN));
		} else {
			gc.setBackground(background);
		}
		gc.fillRectangle(event.x, event.y + 1, fillWidth, event.height - 2);

		// Foreground: text label
		String text = getText(element);
		if (foreground == null) gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		else gc.setForeground(foreground);
		int textHeight = gc.textExtent(text).y;
		gc.drawText(text, event.x + 4, event.y + (event.height/2) - textHeight/2, true);
	}

	protected String getText(Object element) {
		return "";
	}

	protected double getPercentage(Object element) {
		return 0;
	}

}
