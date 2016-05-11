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
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class LineProvider extends BaseFormProvider {
	
	private boolean drawing;
	private Point mousePoint;
	private double currentLength;
	
	@Override
	public String getShapeName() {
		return "Lines";
	}
	
	@Override
	public void onMouseDown(int x, int y) {
		if (!drawing) startPath(x, y);
		else addPointToPath(x, y);
	}

	@Override
	public void onMouseMove(int x, int y) {
		if (drawing) resumePath(x, y);
	}
	
	@Override
	public void onKeyPress(int keyCode) {
		if (drawing && keyCode == SWT.CR) endPath();
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
			if (labelImg) {
				Color c = colorStore.get(new RGB(labelIndex,labelIndex,labelIndex));
				gc.setForeground(c);
				gc.setBackground(c);
				labelIndex++;
				if (labelIndex > 255) labelIndex = 2;
			}
			Path p = new Path(gc.getDevice(), path);
			gc.drawPath(p);
			p.dispose();
		}
		
		if (getCurrentPath() != null) {
			gc.drawPath(getCurrentPath());
			if (mousePoint != null) {
				Point start = getLastPathPoint();
				gc.fillOval(start.x - 3, start.y - 3, 6, 6);
				gc.drawLine(start.x, start.y, mousePoint.x, mousePoint.y);
				gc.fillOval(mousePoint.x - 3, mousePoint.y - 3, 6, 6);
				
				// Calculate total length to cursor.
				double tempLength = currentLength + SWTUtils.getDistance(start, mousePoint);
				gc.drawText(NumberUtils.round(tempLength, 2), mousePoint.x+13, mousePoint.y+3, true);
			}
		}
	}

	private void startPath(int x, int y) {
		drawing = true;
		currentLength = 0;
		Point point = getPointTranslator().screenToImage(x, y);
		Path path = new Path(null);
		setCurrentPath(path);
		path.moveTo(point.x, point.y);
		getEventManager().shapeStarted(point.x, point.y);
	}
	
	private void addPointToPath(int x, int y) {
		Point lastPoint = getLastPathPoint();
		Point newPoint = getPointTranslator().screenToImage(x, y);
		double length = SWTUtils.getDistance(lastPoint, newPoint);
		currentLength += length;
		getCurrentPath().lineTo(newPoint.x, newPoint.y);
		getEventManager().shapeResumed(newPoint.x, newPoint.y);
	}
	
	private void resumePath(int x, int y) {
		mousePoint = getPointTranslator().screenToImage(x, y);
		getEventManager().shapeResumed(mousePoint.x, mousePoint.y);
	}
	
	private void endPath() {
		Path currentPath = getCurrentPath();
		if (currentPath == null || currentPath.isDisposed()) return;
		setCurrentPath(null);
		
		PathData pathData = currentPath.getPathData();
		getPathData().add(pathData);
		getEventManager().shapeFinished(pathData);
		
		currentPath.dispose();
		currentPath = null;
		mousePoint = null;
		drawing = false;
	}
	
	private Point getLastPathPoint() {
		if (getCurrentPath() == null) return null;
		float[] s = new float[2];
		getCurrentPath().getCurrentPoint(s);
		Point point = new Point((int)s[0],(int)s[1]);
		return point;
	}
}
