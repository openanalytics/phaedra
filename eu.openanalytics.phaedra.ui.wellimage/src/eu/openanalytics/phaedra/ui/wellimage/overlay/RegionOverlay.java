package eu.openanalytics.phaedra.ui.wellimage.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class RegionOverlay extends JP2KOverlay {

	private List<PathData> regions;
	private Map<PathData, RGB> regionColors;
	
	private ColorStore colorStore;
	
	public RegionOverlay() {
		super();
		regions = new ArrayList<>();
		regionColors = new HashMap<>();
		colorStore = new ColorStore();
	}
	
	public void addRegion(PathData region) {
		regions.add(region);
	}
	
	public void addRegion(PathData region, RGB color) {
		regions.add(region);
		regionColors.put(region, color);
	}
	
	public void removeRegion(PathData region) {
		regions.remove(region);
		regionColors.remove(region);
	}
	
	public void addRegions(PathData[] regions, RGB[] colors) {
		for (int i=0; i<regions.length; i++) {
			this.regions.add(regions[i]);
			this.regionColors.put(regions[i], colors[i]);
		}
	}
	
	public void clearRegions() {
		regions.clear();
		regionColors.clear();
	}
	
	@Override
	public void render(GC gc) {
		for (PathData region: regions) {
			drawRegion(region, gc);
		}
	}
	
	@Override
	public void dispose() {
		colorStore.dispose();
		super.dispose();
	}
	
	private void drawRegion(PathData region, GC gc) {
		Point[] pts = SWTUtils.getPoints(region);
		for (int i=0; i<pts.length; i++) {
			pts[i] = translate(pts[i]);
		}
		Path path = SWTUtils.createPath(pts);
		
		RGB rgb = regionColors.get(region);
		Color c = null;
		if (rgb == null) {
			c = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);
		} else {
			c = colorStore.get(rgb);
		}
		
		gc.setForeground(c);
		gc.setBackground(c);
		gc.setAlpha(50);
		gc.fillPath(path);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setAlpha(255);
		gc.drawPath(path);
		path.dispose();
	}
}
