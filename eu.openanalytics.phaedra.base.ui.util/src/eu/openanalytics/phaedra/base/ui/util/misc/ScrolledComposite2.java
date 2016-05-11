package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ScrollBar;

import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

/**
 * Based on the SWT ScrolledComposite, but without the height limitation of 32768 pixels.
 * To use the horizontal scrollbar, provide the SWT.H_SCROLL style bit and call setTotalWidth(int).
 * To use the vertical scrollbar, provide the SWT.V_SCROLL style bit and call setTotalHeight(int).
 */
public class ScrolledComposite2 extends Canvas {

	private GridLayout layoutDelegate;
	private Point[] controlPositions;
	private int offsetX;
	private int offsetY;
	
	private int totalWidth;
	private int totalHeight;
	
	public ScrolledComposite2(Composite parent, int style) {
		super(parent, style);
		super.setLayout(new ScrolledComposite2Layout());
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(this);
		
		ScrollBar hBar = getHorizontalBar();
		if (hBar != null) hBar.addListener(SWT.Selection, e -> hScroll());
		
		ScrollBar vBar = getVerticalBar();
		if (vBar != null) vBar.addListener(SWT.Selection, e -> vScroll());
	}
	
	public void setTotalWidth(int totalWidth) {
		this.totalWidth = totalWidth;
	}
	
	public void setTotalHeight(int totalHeight) {
		this.totalHeight = totalHeight;
	}
	
	@Override
	public void setLayout(Layout layout) {
		if (!(layout instanceof GridLayout)) throw new IllegalArgumentException("Only GridLayout is supported");
		layoutDelegate = (GridLayout)layout;
	}
	
	@Override
	public Layout getLayout() {
		return layoutDelegate;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void positionChildren() {
		if (controlPositions == null || controlPositions.length == 0) return;
		Control[] children = getChildren();
		Rectangle area = getClientArea();
		for (int i = 0; i < controlPositions.length; i++) {
			Point pos = controlPositions[i];
			Point size = children[i].getSize();
			Point visibleWidth = new Point(offsetX, offsetX + area.width);
			Point visibleHeight = new Point(offsetY, offsetY + area.height);
			if (SWTUtils.overlaps(visibleWidth, new Point(pos.x, pos.x + size.x)) && SWTUtils.overlaps(visibleHeight, new Point(pos.y, pos.y + size.y))) {
				children[i].setVisible(true);
				children[i].setLocation(pos.x - offsetX, pos.y - offsetY);
			} else {
				children[i].setVisible(false);
			}
		}
	}
	
	private void hScroll() {
		offsetX = getHorizontalBar().getSelection();
		positionChildren();
	}
	
	private void vScroll() {
		offsetY = getVerticalBar().getSelection();
		positionChildren();
	}
	
	private class ScrolledComposite2Layout extends Layout {

		private boolean layoutInProgress;
		
		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			return composite.getSize();
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			if (layoutInProgress) return;
			layoutInProgress = true;
			GridLayout gl = (GridLayout)layoutDelegate;
			Control[] children = composite.getChildren();
			controlPositions = new Point[children.length];
			
			Rectangle area = composite.getClientArea();
			int availableWidth = (totalWidth == 0) ? area.width : totalWidth;
			int availableHeight = (totalHeight == 0) ? area.height : totalHeight;
			
			int rows = children.length / gl.numColumns;
			if (children.length % gl.numColumns != 0) rows++;
			if (rows == 0) return;
			
			int widthPerChild = (availableWidth / gl.numColumns) - gl.horizontalSpacing;
			int heightPerChild = (availableHeight / rows) - gl.verticalSpacing;

			ScrollBar hBar = getHorizontalBar();
			if (hBar != null) {
				if (totalWidth > area.width) {
					hBar.setVisible(true);
					hBar.setThumb(area.width);
					hBar.setMaximum(totalWidth);
					if ((hBar.getSelection() + area.width) >= totalWidth) {
						offsetX = totalWidth - area.width - 1; 
						hBar.setSelection(offsetX);
					}
				} else {
					hBar.setVisible(false);
				}
			}
			
			ScrollBar vBar = getVerticalBar();
			if (vBar != null) {
				if (totalHeight > area.height) {
					vBar.setVisible(true);
					vBar.setThumb(area.height);
					vBar.setMaximum(totalHeight);
					if ((vBar.getSelection() + area.height) >= totalHeight) {
						offsetY = totalHeight - area.height - 1; 
						vBar.setSelection(offsetY);
					}
				} else {
					getVerticalBar().setVisible(false);
				}
			}
			
			int currentX = gl.marginWidth;
			int currentY = gl.marginHeight;
			for (int i = 0; i < children.length; i++) {
				children[i].setSize(widthPerChild, heightPerChild);
				controlPositions[i] = new Point(currentX, currentY);
				// Calculate position of next child (if any).
				currentX += widthPerChild + gl.horizontalSpacing;
				if ((i+1) % gl.numColumns == 0) {
					currentX = gl.marginWidth;
					currentY += heightPerChild + gl.verticalSpacing;
				}
			}
			positionChildren();
			layoutInProgress = false;
		}
	}
}
