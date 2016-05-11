package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import org.eclipse.jface.viewers.BaseLabelProvider;

public abstract class AbstractThumbnailLabelProvider extends BaseLabelProvider {

	public IThumbnailCellRenderer createCellRenderer() {
		// Default: no renderer.
		return null;
	}
	
}