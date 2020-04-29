package eu.openanalytics.phaedra.base.ui.charting.v2.chart.density;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import uk.ac.starlink.ttools.plot.DensityLegend;
import uk.ac.starlink.ttools.plot.DensityPlot;

public class Density2DLegendItem<ENTITY, ITEM> extends AbstractLegendItem<ENTITY, ITEM> {

	private static final DecimalFormat df = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.US));

	private DensityLegend auxLegend;
	private AbstractChartLayer<ENTITY, ITEM> layer;
	private AuxiliaryChartSettings auxChartSettings;

	private Integer[] drag = new Integer[2];
	private Double[] loHiCuts = new Double[2];

	public Density2DLegendItem(AbstractLegend<ENTITY, ITEM> parent, String name, boolean enabled, boolean auxilaryData) {
		super(parent, name, enabled, auxilaryData);

		this.layer = parent.getLayer();
		this.auxChartSettings = getAuxiliaryChartSettings(this, layer.getChartSettings());
	}

	/**
	 * Get the plot image as an buffered image.
	 *
	 * @return The buffered image
	 */
	@Override
	public Image getAuxilaryImage(int width, int height) {
		// Create Topcat legend
		auxLegend = new DensityLegend(true, 12);
		final DensityPlot plot = (DensityPlot) layer.getChart().getPlot();
		if (plot != null && plot.getState() != null && plot.getBinnedData() != null) {
			auxLegend.configure(plot.getState(), plot.getBinnedData());
		}

		// Convert to image
		final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		// Avoid SWT-AWT deadlock in Mac caused by frame.dispose
		if (!ProcessUtils.isMac()) {
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
		}

		return AWTImageConverter.convert(Display.getCurrent(), img);
	}

	@Override
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

	@Override
	public MouseMoveListener getMouseMoveListener() {
		return new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (drag[0] != null) {
					Control source = (Control) e.getSource();
					drag[1] = e.x;
					double cutPerPixel = auxLegend.getValue() / source.getSize().x;
					loHiCuts[0] = auxLegend.getFrac(cutPerPixel * drag[0]);
					loHiCuts[1] = auxLegend.getFrac(cutPerPixel * drag[1]);
					if (loHiCuts[0] < loHiCuts[1]) {
						auxChartSettings.setLoCut(Math.max(0.0d, loHiCuts[0]));
						auxChartSettings.setHiCut(Math.min(1.0d, loHiCuts[1]));
					} else {
						auxChartSettings.setLoCut(0.0d);
						auxChartSettings.setHiCut(1.0d);
					}
					layer.getChart().dataChanged();
					source.redraw();
				}
			}
		};
	}

	@Override
	public MouseListener getMouseListener(final BaseLegendView<?, ?> legendView) {
		return new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				drag[0] = null;
				legendView.setLockedForRefresh(false);
				layer.getChart().dataChanged();
			}

			@Override
			public void mouseDown(MouseEvent e) {
				drag[0] = e.x;
				legendView.setLockedForRefresh(true);
			}
		};
	}

	private AuxiliaryChartSettings getAuxiliaryChartSettings(AbstractLegendItem<ENTITY, ITEM> legendItem, ChartSettings settings) {
		for (AuxiliaryChartSettings setting : settings.getAuxiliaryChartSettings()) {
			if (setting.getWeightFeature() == null || setting.getWeightFeature().equals(legendItem.getName())) {
				return setting;
			}
		}
		return null;
	}

}