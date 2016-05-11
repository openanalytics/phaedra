package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import javax.swing.JComponent;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour.Gridder;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import uk.ac.starlink.ttools.plot.BinGrid;
import uk.ac.starlink.ttools.plot.DensityPlotState;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotDataPointIterator;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PlotSurface;
import uk.ac.starlink.ttools.plot.PointIterator;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.SurfacePlot;

public class ScatterDensityPlot extends SurfacePlot {

	private static final long serialVersionUID = -8216273740249012992L;

	private BinGrid[] grids_;
	private double[] gridLoBounds_;
	private double[] gridHiBounds_;

	private int nPotential_;
	private int nVisible_;

	private BufferedImage image_;
	private BitSet selectionBitSet_;

	private Rectangle lastPlotZone_;
	private PlotState lastState_;
	private double[][] lastRanges_;

	private int plotTime_;
	private boolean forceRedraw;

	public ScatterDensityPlot(PlotSurface surface) {
		super();
		setPreferredSize(new Dimension(400, 400));
		add(new ScatterDensityPanel());
		setSurface(surface);
	}

	public PointIterator getPlottedPointIterator() {
		return new PlotDataPointIterator(getState().getPlotData(), getPointPlacer());
	}

	public PointPlacer getPointPlacer() {
		final PlotSurface surface = getSurface();
		return new PointPlacer() {
			@Override
			public Point getXY(double[] coords) {
				return surface.dataToGraphics(coords[0], coords[1], true);
			}
		};
	}

	public void setSelectionBitSet(BitSet selectionBitSet) {
		this.selectionBitSet_ = selectionBitSet;
	}

	public BitSet getSelectionBitSet() {
		return selectionBitSet_;
	}

	private class ScatterDensityPanel extends JComponent {

		private static final long serialVersionUID = 86073445511069799L;

		ScatterDensityPanel() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();

			BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gim = im.createGraphics();

			getSurface().paintSurface(gim);
			drawData(gim);

			boolean done = g.drawImage(im, 0, 0, null);
			assert done;
		}

		@Override
		protected void printComponent(Graphics g) {

			/*
			 * Tweak so that exact positioning of lines between pixel and
			 * graphics plotting doesn't look wrong. Possibly this is only
			 * required for (a bug in) org.jibble.epsgraphics.EpsGraphics2D?
			 */
			if (isVectorContext(g)) {
				Rectangle clip = getSurface().getClip().getBounds();
				int cx = clip.x - 2;
				int cy = clip.y - 2;
				int cw = clip.width + 4;
				int ch = clip.height + 4;
				g.clearRect(cx, cy, cw, ch);
			}
			super.printComponent(g);
		}
	}

	private void drawData(Graphics2D g2) {
		Rectangle plotZone = getSurface().getClip().getBounds();
		PlotState state = getState();
		if (plotZone.isEmpty() || state == null) {
			return;
		}

		Shape clip = g2.getClip();
		Color color = g2.getColor();

		if (state.getValid()) {
			BufferedImage im = getImage(plotZone, (DensityPlotState)state);
			g2.setClip(plotZone);
			g2.drawImage(im, plotZone.x, plotZone.y, null);
		} else {
			g2.setColor(Color.BLACK);
			g2.fillRect(plotZone.x, plotZone.y, plotZone.width, plotZone.height);
		}
		g2.setClip(clip);
		g2.setColor(color);
		firePlotChangedLater(new PlotEvent(this, state, nPotential_, selectionBitSet_ != null ? selectionBitSet_.cardinality() : 0, nVisible_));
	}

	private BufferedImage getImage(Rectangle plotZone, DensityPlotState state) {

		double[][] ranges = state.getRanges();
		if (image_ == null || !plotZone.equals(lastPlotZone_) || !state.equals(lastState_) || !ranges.equals(lastRanges_) || forceRedraw) {

			boolean weighted = state.getWeighted();
			double loCut = state.getLoCut();
			double hiCut = state.getHiCut();

			int xsize = plotZone.width;
			int ysize = plotZone.height;
			BufferedImage image = new BufferedImage(xsize, ysize, BufferedImage.TYPE_INT_ARGB);

			// Note: always bin using pixelSize = 1, or the scatter coordinates will be off!
			doBinning(plotZone, 1);
			BinGrid[] grids = grids_;

			PlotData data = state.getPlotData();
			int nset = data.getSetCount();
			Style[] styles = new Style[nset];
			for (int is = 0; is < nset; is++) {
				styles[is] = data.getSetStyle(is);
			}
			if (grids.length > 1) {
				grids = grids.clone();
				styles = styles.clone();
				foldGrids(grids, styles);
			}

			for (int is = 0; is < grids.length; is++) {
				BinGrid grid = grids[is];
				if (grid != null) {
					double lo = grid.getCut(loCut);
					double hi = grid.getCut(hiCut);
					if (lo == 1 && !weighted) lo = 0;

					short[] bdata = grid.getBytes(lo, hi, false);
					double[] dataArray = new double[bdata.length];
					for (int i=0; i<bdata.length; i++) {
						dataArray[i] = (bdata[i] == Short.MAX_VALUE)? 0 : bdata[i];
					}

					MarkShape shape = MarkShape.FILLED_CIRCLE;
					Pixellator pixellator = shape.getStyle(Color.BLACK, state.getPixelSize(), 1.0f).getPixelOffsets();
					Gridder gridder = new Gridder(xsize, ysize);
					int[] convolved = convolve(dataArray, gridder, pixellator);

					// Create color map
					int colorMapSize = 256;
					Shader shader = state.getIndexedShader();
					byte[] red = new byte[colorMapSize];
		            byte[] green = new byte[colorMapSize];
		            byte[] blue = new byte[colorMapSize];
		            float[] rgb = new float[4];
		            float scale = 1f / (colorMapSize - 1);
		            for (int i = 1; i < colorMapSize; i++ ) {
		                rgb[3] = 1f;
		                double level = (i - 1) * scale;
		                shader.adjustRgba(rgb, (float) level);
		                red[i] = (byte) (rgb[ 0 ] * 255);
		                green[i] = (byte) (rgb[ 1 ] * 255);
		                blue[i] = (byte) (rgb[ 2 ] * 255);
		            }

		            WritableRaster raster = image.getRaster();

		            // Apply color map
					for (int i=0; i<convolved.length; i++) {
						if (convolved[i] == 0) continue;
						int scaledValue = Math.min(convolved[i]/2, colorMapSize-1);
						// Fix: prevent binned values from 'falling off'
						if (scaledValue == 0 && convolved[i] == 1) scaledValue = 1;
						raster.setSample(i%xsize, i/xsize, 0, red[scaledValue]);
						raster.setSample(i%xsize, i/xsize, 1, green[scaledValue]);
						raster.setSample(i%xsize, i/xsize, 2, blue[scaledValue]);
						raster.setSample(i%xsize, i/xsize, 3, 255);
					}
				}
			}

			forceRedraw = state.isDragging();
			image_ = image;
			lastPlotZone_ = plotZone;
			lastState_ = state;
			lastRanges_ = CollectionUtils.copyOf(ranges);
		}

		return image_;
	}

	/**
	 * Takes an array of grids and corresponding styles and arranges it so that
	 * if any of the styles are the same (represent the same colour channel)
	 * then they are combined by summing their count arrays. On output the
	 * elements of the <code>grids</code> and <code>styles</code> arrays may be
	 * replaced by new values; these arrays may end up with fewer elements than
	 * on input. In this case, some elements will contain nulls on exit.
	 *
	 * @param grids
	 *            input array of data grids (modified on exit)
	 * @param styles
	 *            input array of plotting DensityStyles corresponding to
	 *            <code>grids</code> (modified on exit)
	 */
	private void foldGrids(BinGrid[] grids, Style[] styles) {
		int ngrid = grids.length;
		if (styles.length != ngrid) {
			throw new IllegalArgumentException();
		}
		BinGrid[] rgbGrids = new BinGrid[3];
		DensityStyle[] rgbStyles = new DensityStyle[3];
		List<DensityStyle> seenStyles = new ArrayList<>();
		for (int is = 0; is < ngrid; is++) {
			int iseen = seenStyles.indexOf(styles[is]);
			if (iseen < 0) {
				DensityStyle style = (DensityStyle) styles[is];
				iseen = seenStyles.size();
				seenStyles.add(style);
				rgbGrids[iseen] = grids[is];
				rgbStyles[iseen] = style;
			} else {
				double[] c1 = grids[is].getSums();
				double[] c0 = rgbGrids[iseen].getSums();
				int npix = c0.length;
				assert npix == c1.length;
				for (int i = 0; i < npix; i++) {
					c0[i] += c1[i];
				}
				rgbGrids[iseen].recalculate();
			}
		}
		Arrays.fill(grids, null);
		Arrays.fill(styles, null);
		for (int irgb = 0; irgb < seenStyles.size(); irgb++) {
			grids[irgb] = rgbGrids[irgb];
			styles[irgb] = rgbStyles[irgb];
		}
	}

	private void doBinning(Rectangle zone, int pixsize) {

		/* Work out the data space bounds of the required grid. */
		DensityPlotState state = (DensityPlotState) getState();
		boolean xflip = state.getFlipFlags()[0];
		boolean yflip = state.getFlipFlags()[1];
		boolean xlog = state.getLogFlags()[0];
		boolean ylog = state.getLogFlags()[1];
		double[] loBounds = getSurface().graphicsToData(xflip ? zone.x + zone.width : zone.x,
				yflip ? zone.y : zone.y + zone.height, false);
		double[] hiBounds = getSurface().graphicsToData(xflip ? zone.x : zone.x + zone.width,
				yflip ? zone.y + zone.height : zone.y, false);

		/*
		 * See if we already have BinGrids with the right characteristics. If
		 * not, we have to calculate one.
		 */
		int xsize = zone.width;
		int ysize = zone.height;
		int xpix = (xsize + pixsize - 1) / pixsize;
		int ypix = (ysize + pixsize - 1) / pixsize;

		if (grids_ == null || grids_.length == 0 || grids_[0].getSizeX() != xpix || grids_[0].getSizeY() != ypix
				|| !Arrays.equals(loBounds, gridLoBounds_) || !Arrays.equals(hiBounds, gridHiBounds_)
				|| !state.equals(lastState_) || forceRedraw) {

			double xBin = xsize / (xlog ? Math.log(hiBounds[0] / loBounds[0]) : (hiBounds[0] - loBounds[0])) / pixsize;
			double yBin = ysize / (ylog ? Math.log(hiBounds[1] / loBounds[1]) : (hiBounds[1] - loBounds[1])) / pixsize;

			PlotData data = state.getPlotData();
			int nset = data.getSetCount();
			BinGrid[] grids;

			/*
			 * Decide if we're working in monochrome mode (sum all subset
			 * counts) or RGB mode (accumulate counts for different subsets into
			 * different bin grids).
			 */
			boolean sumAll = !state.getRgb();
			if (sumAll) {
				grids = new BinGrid[] { new BinGrid(xpix, ypix, false) };
			} else {
				grids = new BinGrid[nset];
				for (int is = 0; is < nset; is++) {
					grids[is] = new BinGrid(xpix, ypix, false);
				}
			}

			/* Start the stopwatch to time how long it takes to plot all points. */
	        long tStart = System.currentTimeMillis();

	        /* If this is an intermediate plot (during a drag, with a
	         * guaranteed non-intermediate one to follow shortly), then prepare
	         * to plot only a fraction of the points.  In this way we can
	         * ensure reasonable responsiveness - dragging the plot will not
	         * take ages between frames. */
	        boolean isDragging = state.isDragging();
	        int step = isDragging ? Math.max( plotTime_, 1 ) : 1;

			boolean[] setFlags = new boolean[nset];
			int nVisible = 0;
			PointSequence pseq = data.getPointSequence();
			int ip = 0;
			while ( pseq.next() ) {
				boolean use = false;
				for (int is = 0; is < nset; is++) {
					boolean inc = pseq.isIncluded(is);
					setFlags[is] = inc;
					use = use || inc;
				}
				if (use) {
					double[] coords = pseq.getPoint();
					if (!Double.isNaN(coords[0]) && !Double.isNaN(coords[1]) && !Double.isNaN(coords[2])
							&& !Double.isInfinite(coords[0]) && !Double.isInfinite(coords[1])
							&& !Double.isInfinite(coords[2]) && !(xlog && coords[0] <= 0.0)
							&& !(ylog && coords[1] <= 0.0)) {
						int ix = (int) Math.floor(xBin
								* (xlog ? Math.log(coords[0] / loBounds[0]) : (coords[0] - loBounds[0])));
						int iy = (int) Math.floor(yBin
								* (ylog ? Math.log(coords[1] / loBounds[1]) : (coords[1] - loBounds[1])));
						if (ix >= 0 && ix < xpix && iy >= 0 && iy < ypix) {
							nVisible++;
							if (xflip) {
								ix = xpix - 1 - ix;
							}
							if (yflip) {
								iy = ypix - 1 - iy;
							}
							for (int is = 0; is < nset; is++) {
								if (setFlags[is]) {
									grids[sumAll ? 0 : is].submitDatum(ix, iy, coords[2]);
								}
							}
						}
					}
				}
				while ( ( ++ip % step ) != 0 && pseq.next() );
			}
			pseq.close();

			/* Calculate time to plot all points. */
	        if ( ! isDragging ) {
	            plotTime_ = (int) ( System.currentTimeMillis() - tStart );
	        }

			grids_ = grids;
			gridLoBounds_ = loBounds;
			gridHiBounds_ = hiBounds;
			nPotential_ = ip;
			nVisible_ = nVisible;
		}
	}

	/**
     * Convolves a grid of bin counts with a marker shape to produce
     * a grid of values indicating how many pixels would be plotted
     * per grid position if the given shape had been stamped down once
     * for each entry in the bin count grid.
     * To put it another way, the marker shape acts as a (shaped top hat)
     * smoothing kernel.
     *
     * @param   binner  contains pixel counts per grid point
     * @param   gridder  contains grid geometry
     * @param  pixellator  marker shape in terms of pixel iterator
     */
    private static int[] convolve( double[] dataArray, Gridder gridder,
                                   Pixellator pixellator ) {
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        int[] buf = new int[ gridder.getLength() ];
        for ( pixellator.start(); pixellator.next(); ) {
            int px = pixellator.getX();
            int py = pixellator.getY();
            int ix0 = Math.max( 0, px );
            int ix1 = Math.min( nx, nx + px );
            int iy0 = Math.max( 0, py );
            int iy1 = Math.min( ny, ny + py );
            for ( int iy = iy0; iy < iy1; iy++ ) {
                int jy = iy - py;
                for ( int ix = ix0; ix < ix1; ix++ ) {
                    int jx = ix - px;
                    buf[ gridder.getIndex( ix, iy ) ] += (int)dataArray[gridder.getIndex( jx, jy )];
                }
            }
        }
        return buf;
    }
}