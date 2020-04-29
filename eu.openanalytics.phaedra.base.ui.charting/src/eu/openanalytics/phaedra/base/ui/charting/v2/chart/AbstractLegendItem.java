package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import uk.ac.starlink.ttools.plot.AuxLegend;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.Style;

public abstract class AbstractLegendItem<ENTITY, ITEM> {

	private static final DecimalFormat df = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.US));

	public static final String NAME = "Name";
	public static final String OPACITY = "Transparancy";
	public static final String SIZE = "Size";
	private AbstractLegend<ENTITY, ITEM> parent;
	private boolean enabled = true;
	private String name;
	private boolean auxilaryData;
	private Image iconImage;
	private Image auxLegendImage;
	private int order;

	private AuxLegend auxLegend;
	private AuxiliaryChartSettings auxChartSettings;
	private double pixelsPerCut;
	private Integer[] drag = new Integer[2];
	private Double[] loHiCuts = new Double[2];

	public AbstractLegendItem(AbstractLegend<ENTITY, ITEM> parent, String name, boolean enabled, boolean auxilaryData) {

		this.parent = parent;
		this.enabled = enabled;
		this.name = name;
		this.auxilaryData = auxilaryData;
	}

	public Image getIconImage() {
		if (iconImage == null) {
			iconImage = createIconImage();
		}
		return iconImage;
	}

	public Image createIconImage() {
		return null;
	}

	/**
	 * get the plot image as an buffered image
	 *
	 * @return the buffered image
	 */
	public Image getAuxilaryImage(int width, int height) {

		// Create topcat legend
		auxLegend = new AuxLegend(true, 12);

		final PlotState plotState = getParent().getLayer().getChart().getPlotState();
		List<String> auxilaryFeatures = getParent().getLayer().getDataProvider().getAuxiliaryFeatures();
		int selectedFeatureCount = getParent().getLayer().getDataProvider().getSelectedFeatures().size();
		for (int position = 0; position < auxilaryFeatures.size(); position++) {
			if (name.equals(getParent().getLayer().getDataProvider().getSelectedFeature(selectedFeatureCount + position))) {
				auxLegend.configure(plotState, position);
				auxChartSettings = getParent().getLayer().getChart().getChartSettings().getAuxiliaryChartSettings()
						.get(position);
				break;
			}
		}

		// Convert to image
		final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.createGraphics();
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setPreferredSize(new Dimension(width, height));
		frame.setBackground(Color.WHITE);
		frame.add(auxLegend);
		auxLegend.setBackground(Color.WHITE);
		auxLegend.setOpaque(true);
		frame.pack();
		frame.setVisible(false);
		auxLegend.paint(g);
		g.dispose();
		frame.dispose();

		auxLegendImage = AWTImageConverter.convert(Display.getCurrent(), img);
		return auxLegendImage;
	}

	public PaintListener getPaintListener() {
		return new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (drag[0] != null && drag[1] != null) {
					Control source = (Control) e.getSource();
					e.gc.setBackground(e.gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
					e.gc.drawLine(drag[0], 0, drag[0], source.getSize().y);
					e.gc.drawString(df.format(loHiCuts[0]), drag[0] + 5, 10);
					e.gc.drawLine(drag[1], 0, drag[1], source.getSize().y);
					e.gc.drawString(df.format(loHiCuts[1]), drag[1] + 5, 10);
				}
			}
		};
	}

	public MouseMoveListener getMouseMoveListener() {
		return new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				if (drag[0] != null) {
					Control source = (Control) e.getSource();
					drag[1] = e.x;
					double cutPerPixel = pixelsPerCut / source.getSize().x;
					loHiCuts[0] = cutPerPixel * drag[0];
					loHiCuts[1] = cutPerPixel * drag[1];
					if (loHiCuts[0] < loHiCuts[1]) {
						auxChartSettings.setLoCut(Math.max(0.0d, loHiCuts[0]));
						auxChartSettings.setHiCut(Math.min(1.0d, loHiCuts[1]));
					} else {
						auxChartSettings.setLoCut(0.0d);
						auxChartSettings.setHiCut(1.0d);
					}
					getParent().getLayer().getChart().dataChanged();
					source.redraw();
				}
			}
		};
	}

	public MouseListener getMouseListener(final BaseLegendView<?, ?> legendView) {
		return new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				drag[0] = null;
				legendView.setLockedForRefresh(false);
				getParent().getLayer().getChart().dataChanged();
			}

			@Override
			public void mouseDown(MouseEvent e) {
				drag[0] = e.x;
				pixelsPerCut = auxChartSettings.getHiCut() - auxChartSettings.getLoCut();
				legendView.setLockedForRefresh(true);
			}
		};
	}

	public AbstractLegend<ENTITY, ITEM> getParent() {
		return parent;
	}

	public void setParent(AbstractLegend<ENTITY, ITEM> parent) {
		this.parent = parent;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Style getStyle() {
		return getParent().getChartSettings().getStyle(getName());
	}

	public boolean canModify(String property) {
		return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPropertyValue(String property) {
		if (NAME.equals(property)) {
			return getName();
		}
		return "";
	}

	public void setPropertyValue(String property, String value) {
		// Do nothing.
	}

	public boolean hasAuxilaryData() {
		return auxilaryData;
	}

	public void disposeImages() {
		if (iconImage != null) {
			iconImage.dispose();
		}
		if (auxLegendImage != null) {
			auxLegendImage.dispose();
		}
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public static Comparator<AbstractLegendItem<?, ?>> getComparator() {
		return new Comparator<AbstractLegendItem<?, ?>>() {
			@Override
			public int compare(AbstractLegendItem<?, ?> o1, AbstractLegendItem<?, ?> o2) {
				return StringUtils.compareToNumericStrings(o1.getName(), o2.getName());
			}
		};
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + order;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractLegendItem<?, ?> other = (AbstractLegendItem<?, ?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (order != other.order)
			return false;
		return true;
	}

}