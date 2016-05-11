package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

public abstract class AbstractThumbnailCellRenderer implements IThumbnailCellRenderer {

	@Override
	public boolean isImageReady(Object o) {
		return true;
	}

}