package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.List;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PieChartConfig {

	public long[] pieFeatures;
	public long sizeFeature;
	public int[] featureColors;

	public final static int MAX_PIE_FEATURES = 5;

	public final static int[] DEFAULT_PIE_COLORS = {
			0xFF0000, 0x0000FF, 0x00FF00, 0x333399, 0x66FFCC,
			0xFFFF00, 0xFF3300, 0x99FF00, 0x6633FF, 0xFF9933};
	
	public void loadDefaults(long protocolId, String layerId, Plate plate) {

		pieFeatures = new long[MAX_PIE_FEATURES];
		featureColors = new int[MAX_PIE_FEATURES];
		
		for (int i = 0; i < MAX_PIE_FEATURES; i++) {
			// Feature ID
			String featureId = GridState.getStringValue(protocolId, layerId, "pieFeature#" + i);
			if (featureId != null && !featureId.isEmpty()) {
				long id = Long.parseLong(featureId);
				pieFeatures[i] = id;
			}
			
			// Feature Color
			String featureColor = GridState.getStringValue(protocolId, layerId, "pieFeatureColor#" + i);
			if (featureId != null && !featureId.isEmpty()) {
				featureColors[i] = Integer.parseInt(featureColor);
			} else {
				featureColors[i] = DEFAULT_PIE_COLORS[i%DEFAULT_PIE_COLORS.length];
			}
		}
		
		// Size Feature ID
		String sizeFeatureId = GridState.getStringValue(protocolId, layerId, "sizeFeature");
		if (sizeFeatureId != null && !sizeFeatureId.isEmpty()) {
			long id = Long.parseLong(sizeFeatureId);
			sizeFeature = id;
		}
	}
	
	public void saveState(long protocolId, String layerId) {
		for (int i = 0; i < MAX_PIE_FEATURES; i++) {
			GridState.saveValue(protocolId, layerId, "pieFeature#" + i, "" + pieFeatures[i]);
			GridState.saveValue(protocolId, layerId, "pieFeatureColor#" + i, "" + featureColors[i]);
		}
		GridState.saveValue(protocolId, layerId, "sizeFeature", "" + sizeFeature);
	}
	
	public RGB getColor(int i) {
		int color = featureColors[i];
		return new RGB(
				color >> 16 & 0xFF,
				color >> 8 & 0xFF,
				color  & 0xFF
				);
	}
	
	public int getColor(RGB color) {
		return color.red << 16 | color.green << 8 | color.blue;
	}
	
	public Feature getFeature(long id, Plate p) {
		List<Feature> allFeatures = PlateUtils.getFeatures(p);
		for (int i=0; i<allFeatures.size(); i++) {
			if (allFeatures.get(i).getId() == id) return allFeatures.get(i);
		}
		return null;
	}
}
