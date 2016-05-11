package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;

public abstract class ImageLabelProvider extends OwnerDrawLabelProvider {

	private ColumnConfiguration config;

	private boolean autoAdjustSize;
	private TableColumn column;
	private int currentWidth;
	private int currentHeight;
	private int minHeight = 20;

	private Object settings;

	public ImageLabelProvider(ColumnConfiguration config) {
		this(config, true);
	}

	public ImageLabelProvider(ColumnConfiguration config, boolean autoAdjustSize) {
		this.config = config;
		this.autoAdjustSize = autoAdjustSize;
	}

	public Object getSettings() {
		return settings;
	}

	public void setSettings(Object settings) {
		this.settings = settings;
	}

	@Override
	protected void measure(Event event, Object element) {
		if (column == null) initHeightListener((Table)event.widget);
		event.setBounds(new Rectangle(event.x, event.y, currentWidth, currentHeight));
	}

	@Override
	protected void paint(Event event, Object element) {
		if (currentWidth == 0) return;

		ImageData imgData = getImageData(element, settings);
		if (imgData == null) return;

		// Change the width of the column to the widest image found.
		if (config.getWidth() < imgData.width) {
			config.setWidth(imgData.width);
		}

		int x = event.x + (currentWidth - imgData.width)/2;
		int y = event.y + (currentHeight - imgData.height)/2;

		Image img = null;
		try {
			img = new Image(null, imgData);
			if (imgData != null) event.gc.drawImage(img, x-4, y);
		} finally {
			if (img != null) img.dispose();
		}
	}

	protected abstract ImageData getImageData(Object element, Object settings);

	protected void onWidthChange(int newWidth) {
		// Default behaviour: do nothing.
	}

	protected int getCurrentWidth() {
		return currentWidth;
	}

	private void initHeightListener(Table table) {
		for (TableColumn col: table.getColumns()) {
			if (config.getName().equals(col.getText())) {
				column = col;
				break;
			}
		}
		if (column == null) column = table.getColumn(0);

		// Start with a default square size.
		currentWidth = config.getWidth();
		currentHeight = (int) (currentWidth / config.getAspectRatio());

		if (autoAdjustSize) {
			column.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					// In combination with the Resize Table button, the Image column would keep getting 4 pixels larger.
					if (currentWidth+4 == column.getWidth()) {
						currentWidth = config.getWidth();
						currentHeight = (int) (currentWidth / config.getAspectRatio());
						column.setWidth(currentWidth);
					} else {
						currentWidth = column.getWidth();
						currentHeight = (int) (currentWidth / config.getAspectRatio());
					}

					currentHeight = (currentHeight < minHeight) ? minHeight : currentHeight;
					int previousHeight = column.getParent().getItemHeight();
					if (previousHeight != currentHeight) {
						RichTableViewer.setTableItemHeight(column.getParent(), currentHeight);
					}

					onWidthChange(currentWidth);
				}
			});
		}
	}

}