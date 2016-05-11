package eu.openanalytics.phaedra.base.util.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

public class SWTUtils {

	public static float getSurface(PathData path) {
		// Assuming path represents a closed polygon.
		float sum = 0;
		float[] points = new float[path.points.length + 2];
		System.arraycopy(path.points, 0, points, 0, path.points.length);
		points[points.length - 2] = points[0];
		points[points.length - 1] = points[1];
		int n = points.length / 2;
		for (int i=0; i < n-1; i++) {
		    sum = sum + points[2*i] * points[(2*i)+3] - points[(2*i)+1] * points[(2*i)+2];
		}
		sum = Math.abs(sum)/2;
		return sum;
	}
	
	public static float getLength(PathData path) {
		// Assuming path contains only "moveTo" and "lineTo" components.
		double totalLength = 0;
		int pointCount = path.points.length / 2;
		Point previousPoint = null;
		for (int i=0; i<pointCount; i++) {
			Point p = new Point((int)path.points[2*i], (int)path.points[2*i+1]);
			if (previousPoint != null) totalLength += getDistance(previousPoint, p);
			previousPoint = p;
		}
		return (float)totalLength;
	}
	
	public static Point[] getPoints(PathData path) {
		int pointCount = path.points.length / 2;
		Point[] points = new Point[pointCount];
		for (int i=0; i<pointCount; i++) {
			points[i] = new Point((int)path.points[2*i], (int)path.points[2*i+1]);
		}
		return points;
	}
	
	public static Path createPath(Point[] points) {
		Path p = new Path(null);
		if (points.length > 0) {
			p.moveTo(points[0].x, points [0].y);
			for (int i=1; i<points.length; i++) {
				p.lineTo(points[i].x, points[i].y);
			}
			p.close();
		}
		return p;
	}
	
	public static boolean overlaps(Point rangeA, Point rangeB) {
		return (rangeA.y <= rangeB.y && rangeA.y >= rangeB.x) || (rangeB.y <= rangeA.y && rangeB.y >= rangeA.x);
	}
	
	/**
	 * If the Rectangle contains negative width or height,
	 * it is changed so that x, y, width, and height all become positive.
	 * 
	 * @param rect The Rectangle to normalize.
	 */
	public static void normalize(Rectangle rect) {
		if (rect.width < 0) {
			rect.x = rect.x + rect.width;
			rect.width = Math.abs(rect.width);
		}
		if (rect.height < 0) {
			rect.y = rect.y + rect.height;
			rect.height = Math.abs(rect.height);
		}
	}
	
	public static Rectangle create(int[] coords) {
		return create(coords[0],coords[1],coords[2],coords[3]);
	}
	
	public static Rectangle create(int x1, int y1, int x2, int y2) {
		int x = Math.min(x1,x2);
		int y = Math.min(y1,y2);
		int X = Math.max(x1,x2);
		int Y = Math.max(y1,y2);
		Rectangle rect = new Rectangle(x,y,X-x,Y-y);
		return rect;
	}
	
	public static Rectangle copy(Rectangle rectangle) {
		return new Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}
	
	public static Point getCenter(Rectangle rect) {
		int x = rect.x + rect.width/2;
		int y = rect.y + rect.height/2;
		return new Point(x,y);
	}

	public static int getSurface(Rectangle rect) {
		return rect.width * rect.height;
	}

	public static double getDistance(Point a, Point b) {
		double v1 = Math.pow(a.x - b.x, 2);
		double v2 = Math.pow(a.y - b.y, 2);
		return Math.sqrt(v1+v2);
	}
	
	public static void addPadding(Rectangle r, int pad) {
		r.x -= pad/2;
		r.y -= pad/2;
		r.width += pad;
		r.height += pad;
	}
	
	public static int[] getPoints(Rectangle r) {
		return new int[] { r.x, r.y, r.x + r.width, r.y + r.height };
	}
	
	/**
	 * Refresh a TableViewer without losing its selection, even if the
	 * select items have no identity (e.g. newly created features).
	 * 
	 * @param tableViewer The TableViewer to refresh.
	 * @param async True to run in the Display thread, false to run in the current thread.
	 */
	public static void smartRefresh(final TableViewer tableViewer, boolean async) {
		Runnable run = new Runnable() {
			@Override
			public void run() {
				int[] indices = tableViewer.getTable().getSelectionIndices();
				tableViewer.refresh();
				tableViewer.getTable().deselectAll();
				tableViewer.getTable().select(indices);
			}
		};
		if (async) Display.getDefault().asyncExec(run);
		else run.run();
	}
	
	public static StyleRange[] createStyleRanges(int length, TextStyle baseStyle, TextStyle regionStyle, Point[] regions) {
		List<StyleRange> ranges = new ArrayList<>();
		int lastPos = 0;
		if (regions != null && regionStyle != null) {
			Point[] sortedRegions = Arrays.copyOf(regions, regions.length);
			Arrays.sort(sortedRegions, (p1,p2) -> p1.x - p2.x);
			for (int i=0; i<sortedRegions.length; i++) {
				Point region = sortedRegions[i];
				if (baseStyle != null && region.x > lastPos) ranges.add(createStyleRange(lastPos, region.x, baseStyle));
				ranges.add(createStyleRange(region.x, region.y+1, regionStyle));
				lastPos = region.y+1;
			}
		}
		if (baseStyle != null && lastPos < length) ranges.add(createStyleRange(lastPos, length, baseStyle));
		return ranges.toArray(new StyleRange[ranges.size()]);
	}
	
	public static StyleRange createStyleRange(int from, int to, TextStyle style) {
		StyleRange range = new StyleRange(style);
		range.start = from;
		range.length = to - from;
		return range;
	}
}
