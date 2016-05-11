package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.DataConverter;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;

public class ThumbnailTooltip extends AdvancedToolTip {

	private ThumbnailViewer thumbnailViewer;

	public ThumbnailTooltip(ThumbnailViewer thumbnailViewer, ToolTipLabelProvider labelProvider) {
		this(thumbnailViewer, null, labelProvider);
	}

	public ThumbnailTooltip(ThumbnailViewer thumbnailViewer, DataConverter dataConverter, ToolTipLabelProvider labelProvider) {
		super(thumbnailViewer.getThumbnail(), ToolTip.NO_RECREATE, false);
		setPopupDelay(500);
		setShift(new Point(10, 10));
		activate();
		this.thumbnailViewer = thumbnailViewer;

		setDataConverter(dataConverter);
		setLabelProvider(labelProvider);
	}

	@Override
	public Object getData(Event event) {
		Thumbnail thumbnail = thumbnailViewer.getThumbnail();
		ThumbnailCell cell = thumbnail.getCellAt(event.x, event.y);

		if (cell != null) return cell.getData();
		return null;
	}

}
