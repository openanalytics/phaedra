package eu.openanalytics.phaedra.base.imaging.overlay.freeform.impl;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.imaging.overlay.freeform.BaseFormProvider;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;

public class PolygonProvider extends BaseFormProvider {

	private boolean drawing;
	
	@Override
	public String getShapeName() {
		return "Outlines";
	}
	
	@Override
	public void onMouseDown(int x, int y) {
		startPath(x, y);
	}

	@Override
	public void onMouseUp(int x, int y) {
		endPath(x, y);
	}

	@Override
	public void onMouseMove(int x, int y) {
		if (drawing) resumePath(x, y);
	}
	
	@Override
	public void draw(GC gc, boolean labelImg, int startingLabel) {
		gc.setLineStyle(SWT.LINE_SOLID);
		
		int labelIndex = startingLabel + 2;
		ColorStore colorStore = null;
		if (labelImg) {
			colorStore = new ColorStore();
		} else {
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
		}
		
		List<PathData> paths = getPathData();
		for (PathData path: paths) {
			Path p = new Path(gc.getDevice(), path);
			
			if (labelImg) {
				Color c = colorStore.get(new RGB(labelIndex,labelIndex,labelIndex));
				gc.setForeground(c);
				gc.setBackground(c);
				labelIndex++;
				if (labelIndex > 255) labelIndex = 2;
				gc.setAlpha(255);
				gc.fillPath(p);
			} else {
				gc.setAlpha(128);
				gc.fillPath(p);
				gc.setAlpha(255);
				gc.drawPath(p);
			}
			p.dispose();
		}
		
		Path currentPath = getCurrentPath();
		if (currentPath != null && !currentPath.isDisposed()) {
			gc.setAlpha(128);
			gc.fillPath(currentPath);
			gc.setAlpha(255);
			gc.drawPath(currentPath);
		}
		
		if (labelImg) colorStore.dispose();
	}

	private void startPath(int x, int y) {
		drawing = true;
		Path currentPath = getCurrentPath();
		if (currentPath != null && !currentPath.isDisposed()) {
			currentPath.dispose();
		}
		Point p = getPointTranslator().screenToImage(x, y);
		currentPath = new Path(null);
		currentPath.moveTo(p.x, p.y);
		setCurrentPath(currentPath);
		getEventManager().shapeStarted(p.x, p.y);
	}
	
	private void resumePath(int x, int y) {
		Path currentPath = getCurrentPath();
		if (currentPath == null) return;
		Point p = getPointTranslator().screenToImage(x, y);
		currentPath.lineTo(p.x, p.y);
		getEventManager().shapeResumed(x, y);
	}
	
	private void endPath(int x, int y) {
		Path currentPath = getCurrentPath();
		if (currentPath == null || currentPath.isDisposed()) return;
		
		currentPath.close();
		
		PathData pathData = currentPath.getPathData();
		getPathData().add(pathData);
		getEventManager().shapeFinished(pathData);
		
		currentPath.dispose();
		currentPath = null;
		drawing = false;
	}
}
