package eu.openanalytics.phaedra.base.ui.gridviewer;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;

public class GridViewerUtils {

	/**
	 * Should be used to save the Layer config as followed: <code>layer.getName() + CONFIG</code>
	 */
	public static final String CONFIG = "CONFIG";

	/**
	 * <p>Check if the given grid layer support has one of the given {@link IGridLayer}'s enabled.
	 * </p>
	 * @param gridLayerSupport
	 * @param classes
	 * @return
	 */
	@SafeVarargs
	public static boolean hasGridLayerEnabled(GridLayerSupport gridLayerSupport, Class<? extends IGridLayer>... classes) {
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			if (layer.isEnabled()) {
				for (Class<? extends IGridLayer> clazz : classes) {
					if (clazz.isInstance(layer)) return true;
				}
			}
		}
		return false;
	}

}
