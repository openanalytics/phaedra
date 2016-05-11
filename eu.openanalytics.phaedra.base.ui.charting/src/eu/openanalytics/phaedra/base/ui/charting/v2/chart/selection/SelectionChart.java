package eu.openanalytics.phaedra.base.ui.charting.v2.chart.selection;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.NONE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import uk.ac.starlink.ttools.plot.TablePlot;

public class SelectionChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	private MouseAdapter selecter;
	private GeneralPath dragPath;
	private boolean singleSelection;
	private Area area = new Area();
	private Color fillColor = new Color(0, 0, 0, 64);
	private Color pathColor = new Color(0, 0, 0, 128);

	public SelectionChart() {
		setType(NONE);
		setName(SELECTION);
	}

	@Override
	public Component build() {
		chartComponent = new JPanel() {
			private static final long serialVersionUID = -4466990886695870499L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				Color oldColor = g.getColor();
				Graphics2D g2 = (Graphics2D) g;

				Area drawingArea = new Area();
				if (dragPath != null) {
					drawingArea.add(new Area(dragPath));
					g2.setColor(pathColor);
					g2.draw(dragPath);
				}
				g2.setColor(fillColor);
				g2.fill(drawingArea);
				g.setColor(oldColor);
			}
		};
		chartComponent.setOpaque(false);
		chartComponent.addComponentListener(this);
		selecter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.getButton() == MouseEvent.BUTTON1) {
					Point p = evt.getPoint();
					dragPath = new GeneralPath();
					dragPath.moveTo(p.x, p.y);
				}
			}
			@Override
			public void mouseReleased(MouseEvent evt) {
				if (evt.getButton() == MouseEvent.BUTTON1) {
					if (dragPath != null) {
						Rectangle bounds = dragPath.getBounds();
						singleSelection = bounds.width == 0 && bounds.height == 0;
						if (singleSelection) {
							Point p = evt.getPoint();
							area.add(new Area(new Rectangle(p.x - 2, p.y - 2, 4, 4)));
						} else {
							area.add(new Area(dragPath));
						}
						dragPath = null;
					}
					repaint();
				}
			}
			@Override
			public void mouseDragged(MouseEvent evt) {
				if (dragPath != null) {
					Point p = evt.getPoint();
					int x = Math.min(getChartComponent().getWidth(), Math.max(0, p.x));
					int y = Math.min(getChartComponent().getHeight(), Math.max(0, p.y));
					dragPath.lineTo(x, y);
					repaint();
				}
			}
		};
		chartComponent.addMouseListener(selecter);
		chartComponent.addMouseMotionListener(selecter);

		return chartComponent;
	}

	@Override
	public BufferedImage getPlotImage() {
		return null;
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		super.componentResized(arg0);
		clear();
	}

	@Override
	public void updatePlotData() {
		// Do nothing.
	}

	@Override
	public TablePlot createPlot() {
		return null;
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}

	/**
	 * Return the selected Area.
	 * @return
	 */
	public Area getArea() {
		return area;
	}

	/**
	 * A single point was selected instead of an area.
	 * @return
	 */
	public boolean isSingleSelection() {
		return singleSelection;
	}

	/**
	 * Clear the selected area.
	 */
	public void clear() {
		area = new Area();
		repaint();
	}

}