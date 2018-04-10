package eu.openanalytics.phaedra.wellimage;

import java.util.Arrays;

import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class ImageKey extends CacheKey {

	private static final long serialVersionUID = -5219776681397439657L;

	public ImageKey(Well well, float scale, boolean[] channels) {
		super(PlateUtils.getProtocolClass(well).getId(), well.getId(), scale, Arrays.toString(channels));
	}

	public ImageKey(Well well, float scale, boolean[] channels, Rectangle region) {
		super(PlateUtils.getProtocolClass(well).getId(), well.getId(), scale, Arrays.toString(channels), region);
	}

	public ImageKey(Well well, int width, int height, boolean[] channels) {
		super(PlateUtils.getProtocolClass(well).getId(), well.getId(), width, height, Arrays.toString(channels));
	}

	public ImageKey(Well well, Integer cellIndex, float scale, boolean[] channels) {
		super(PlateUtils.getProtocolClass(well).getId(), well.getId(), cellIndex, scale, Arrays.toString(channels));
	}

	public long getProtocolClassId() {
		return (Long) getKeyPart(0);
	}
	
	public long getWellId() {
		return (Long) getKeyPart(1);
	}
}
