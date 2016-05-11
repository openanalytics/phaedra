
package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import javax.swing.JComponent;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import uk.ac.starlink.ttools.plot.BinGrid;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotDataPointIterator;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotSurface;
import uk.ac.starlink.ttools.plot.PointIterator;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.SurfacePlot;

/**
 * This class is based on DensityPlot, with code added from the Topcat 4.0 ContourPlotter.
 */
public class ContourPlot extends SurfacePlot {

	private static final long serialVersionUID = -5183622268745598445L;

	private BinGrid[] grids_;
	private double[] gridLoBounds_;
	private double[] gridHiBounds_;

	private int nPotential_;
	private int nVisible_;

	private BufferedImage image_;
	private BitSet selectionBitSet_;

	private Rectangle lastPlotZone_;
	private ContourPlotState lastState_;
	private double[][] lastRanges_;

	private boolean forceRedraw_;
	private int plotTime_;

	private BufferedImage selectionImage_;
	private Leveller leveller_;
	private double[] dataArrays_;
	private Gridder gridder_;
	private BitSet selLvls_;
	private boolean isSelectionValid_;

	public ContourPlot(PlotSurface surface) {
		super();
		setPreferredSize(new Dimension(400, 400));
		add(new ContourDataPanel());
		setSurface(surface);
		selLvls_ = new BitSet();
	}

	public void setSelectionBitSet(BitSet selectionBitSet) {
		isSelectionValid_ = false;

		if (!selectionBitSet.equals(selectionBitSet_)) {
			// Disabled until further notice.
			//if (getState() == null) return;
			//selLvls_.clear();
			//selectionBitSet_ = selectionBitSet;
			//int ip = -1;
			//PointPlacer placer = getPointPlacer();
			//PlotData plotData = getState().getPlotData();
			//PointSequence pseq = plotData.getPointSequence();
			//int nset = plotData.getSetCount();
			//while ( pseq != null && pseq.next() ) {
			//	ip++;
			//	boolean use = false;
			//	for ( int is = 0; is < nset && ! use; is++ ) {
			//		use = use || pseq.isIncluded( is );
			//	}
			//	if ( use ) {
			//		if (selectionBitSet.get(ip)) {
			//			Point xy = placer.getXY( pseq.getPoint() );
			//			if ( xy != null ) {
			//				int x = xy.x - lastPlotZone_.x;
			//				int y = xy.y - lastPlotZone_.y;
			//				if (x < 0 || y < 0) continue;
			//				int index = gridder_.getIndex( x, y );
			//				if (index < dataArrays_.length) {
			//					int lev = leveller_.getLevel(dataArrays_[index]);
			//					selLvls_.set(lev);
			//				}
			//			}
			//		}
			//	}
			//}
			//pseq.close();
			//pseq = null;

			// Clear levels instead.
			this.selLvls_.clear();
		}

		this.selectionBitSet_ = selectionBitSet;
	}

	public BitSet getSelectionBitSet() {
		return selectionBitSet_;
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

	public BinGrid[] getBinnedData() {
		return grids_;
	}

	private class ContourDataPanel extends JComponent {

		private static final long serialVersionUID = 5668946857797795423L;

		ContourDataPanel() {
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
		ContourPlotState state = (ContourPlotState) getState();
		if (plotZone.isEmpty() || state == null) {
			return;
		}

		Shape clip = g2.getClip();
		Color color = g2.getColor();

		if (state.getValid()) {
			int psize = state.getPixelSize();
			BufferedImage im = getImage(plotZone, state);
			BufferedImageOp scaleOp = new AffineTransformOp(AffineTransform.getScaleInstance(psize, psize), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			g2.setClip(plotZone);
			g2.drawImage(im, scaleOp, plotZone.x, plotZone.y);
		} else {
			g2.setColor(Color.BLACK);
			g2.fillRect(plotZone.x, plotZone.y, plotZone.width, plotZone.height);
		}
		g2.setClip(clip);
		g2.setColor(color);
		firePlotChangedLater(new PlotEvent(this, state, nPotential_, selectionBitSet_ != null ? selectionBitSet_.cardinality() : 0, nVisible_));
	}

	private BufferedImage getImage(Rectangle plotZone, ContourPlotState state) {

		double[][] ranges = state.getRanges();
		if (image_ == null || !plotZone.equals(lastPlotZone_) || !state.equals(lastState_) || !ranges.equals(lastRanges_) || forceRedraw_) {
			boolean weighted = state.getWeighted();
			int xsize = plotZone.width;
			int ysize = plotZone.height;
			int psize = state.getPixelSize();
			int xpix = (xsize + psize - 1) / psize;
			int ypix = (ysize + psize - 1) / psize;

			BufferedImage image = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();

			doBinning(plotZone, psize);
			BinGrid[] grids = grids_;

			double loCut = state.getLoCut();
			double hiCut = state.getHiCut();

			PlotData data = state.getPlotData();
			int nset = data.getSetCount();
			float opacity = 1f;
			Style[] styles = new Style[nset];
			for (int is = 0; is < nset; is++) {
				styles[is] = data.getSetStyle(is);
				opacity = styles[is].getOpacity();
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
					if (lo == 1 && !weighted) {
						lo = 0;
					}

					short[] bdata = grid.getBytes(lo, hi, state.getLogZ());
					dataArrays_ = new double[bdata.length];
					for (int i=0; i<bdata.length; i++) {
						dataArrays_[i] = (bdata[i] == Short.MAX_VALUE)? 0 : bdata[i];
					}

					LevelMode levelMode = LevelMode.LINEAR;
					String levelModeString = state.getLevelMode();
					for (LevelMode mode: LevelMode.MODES) {
						if (mode.toString().equals(levelModeString)) levelMode = mode;
					}
					Color color = new Color(state.getColor().red, state.getColor().green, state.getColor().blue, (int) (255 * opacity));

					ContourStyle style = new ContourStyle(
							color,
							state.getLevels(),
							state.getOffset(),
							state.getSmooth(),
							levelMode);
					gridder_ = new Gridder(xpix, ypix);

					int smooth = style.getSmoothing();
					if ( smooth > 1 ) {
						dataArrays_ = smooth( dataArrays_, gridder_, smooth );
					}

					leveller_ = createLeveller(dataArrays_, style);

					g.setColor(style.getColor());

					for ( int ix = 0; ix < xpix; ix++ ) {
						int lev0 = leveller_.getLevel(dataArrays_[gridder_.getIndex( ix, 0 )]);
						for ( int iy = 1; iy < ypix; iy++ ) {
							int lev1 = leveller_.getLevel(dataArrays_[gridder_.getIndex( ix, iy )]);
							if ( lev1 != lev0 ) {
								g.fillRect(ix, iy - 1, 1, 1 );
							}
							lev0 = lev1;
						}
					}
					for ( int iy = 0; iy < ypix; iy++ ) {
						int lev0 = leveller_.getLevel(dataArrays_[gridder_.getIndex( 0, iy )]);
						for ( int ix = 1; ix < xpix; ix++ ) {
							int lev1 = leveller_.getLevel(dataArrays_[gridder_.getIndex( ix, iy )]);
							if ( lev1 != lev0 ) {
								g.fillRect(ix - 1, iy, 1, 1 );
							}
							lev0 = lev1;
						}
					}
				}
			}

			image_ = image;
			lastPlotZone_ = plotZone;
			lastState_ = state;
			lastRanges_ = CollectionUtils.copyOf(ranges);
			forceRedraw_ = state.isDragging();
			isSelectionValid_ = false;
		}

		// Selection
		if (selLvls_.cardinality() > 0) {
			int xsize = plotZone.width;
			int ysize = plotZone.height;
			int psize = state.getPixelSize();
			int xpix = (xsize + psize - 1) / psize;
			int ypix = (ysize + psize - 1) / psize;

			if (!isSelectionValid_) {
				isSelectionValid_ = true;

				selectionImage_ = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_ARGB);
				Graphics g = selectionImage_.getGraphics();

				Color color = new Color(state.getColor().red, state.getColor().green, state.getColor().blue, 64);
				g.setColor(color);
				for ( int ix = 0; ix < xpix; ix++ ) {
					for ( int iy = 0; iy < ypix; iy++ ) {
						int lev = leveller_.getLevel(dataArrays_[gridder_.getIndex( ix, iy )]);
						if (selLvls_.get(lev)) {
							g.fillRect(ix, iy, 1, 1 );
						}
					}
				}
			}

			BufferedImage img = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_ARGB);
			Graphics g = img.getGraphics();
			g.drawImage(image_, 0, 0, null);
			g.drawImage(selectionImage_, 0, 0, null);
			return img;
		}

		return image_;
	}

	public BitSet getSelectedBitSet(Shape shape) {
		selLvls_.clear();
		selectionBitSet_ = new BitSet();
		isSelectionValid_ = false;

		Area area = new Area(shape);
		// Use bounds of selection so not all pixels need to be checked.
		Rectangle bounds = area.getBounds();

		int psize = lastState_.getPixelSize();
		for ( int ix = bounds.x; ix < bounds.x + bounds.width; ix++ ) {
			for ( int iy = bounds.y; iy < bounds.y + bounds.height; iy++ ) {
				if (area.contains(ix, iy)) {
					int x = (ix - lastPlotZone_.x) / psize;
					int y = (iy - lastPlotZone_.y) / psize;
					if (x > gridder_.getWidth() || y > gridder_.getHeight()) continue;
					int lev1 = leveller_.getLevel(dataArrays_[gridder_.getIndex( x, y )]);
					selLvls_.set(lev1);
				}
			}
		}

		int ip = -1;
		PointPlacer placer = getPointPlacer();
		PlotData plotData = getState().getPlotData();
		PointSequence pseq = plotData.getPointSequence();
		int nset = plotData.getSetCount();
        while ( pseq != null && pseq.next() ) {
            ip++;
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || pseq.isIncluded( is );
            }
            if ( use ) {
                Point xy = placer.getXY( pseq.getPoint() );
                if ( xy != null ) {
                	int x = (xy.x - lastPlotZone_.x) / psize;
    				int y = (xy.y - lastPlotZone_.y) / psize;
    				if (x < 0 || y < 0) continue;
                	int index = gridder_.getIndex( x, y );
                	if (index < dataArrays_.length) {
                		int lev = leveller_.getLevel(dataArrays_[index]);
                		if (selLvls_.get(lev)) {
                			selectionBitSet_.set(ip);
                		}
                	}
                }
            }
        }
        pseq.close();
        pseq = null;

		return selectionBitSet_;
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
		 ContourPlotState state = (ContourPlotState) getState();
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
				 || !state.equals(lastState_) || forceRedraw_) {

			 double xBin = xsize / (xlog ? Math.log(hiBounds[0] / loBounds[0]) : (hiBounds[0] - loBounds[0])) / pixsize;
			 double yBin = ysize / (ylog ? Math.log(hiBounds[1] / loBounds[1]) : (hiBounds[1] - loBounds[1])) / pixsize;

			 PlotData data = state.getPlotData();
			 int nset = data.getSetCount();
			 BinGrid[] grids;

			/* Start the stopwatch to time how long it takes to plot all points. */
	        long tStart = System.currentTimeMillis();

	        /* If this is an intermediate plot (during a drag, with a
	         * guaranteed non-intermediate one to follow shortly), then prepare
	         * to plot only a fraction of the points.  In this way we can
	         * ensure reasonable responsiveness - dragging the plot will not
	         * take ages between frames. */
	        boolean isDragging = state.isDragging();
	        int step = isDragging ? Math.max( plotTime_, 1 ) : 1;

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

	 private static double[] smooth( double[] inArray, Gridder gridder, int smooth ) {
		 int nx = gridder.getWidth();
		 int ny = gridder.getHeight();
		 final double[] out = new double[ gridder.getLength() ];
		 int lo = - smooth / 2;
		 int hi = ( smooth + 1 ) / 2;
		 for ( int px = lo; px < hi; px++ ) {
			 for ( int py = lo; py < hi; py++ ) {
				 int ix0 = Math.max( 0, px );
				 int ix1 = Math.min( nx, nx + px );
				 int iy0 = Math.max( 0, py );
				 int iy1 = Math.min( ny, ny + py );
				 for ( int iy = iy0; iy < iy1; iy++ ) {
					 int jy = iy - py;
					 for ( int ix = ix0; ix < ix1; ix++ ) {
						 int jx = ix - px;
						 out[ gridder.getIndex( ix, iy ) ] +=
								 inArray[gridder.getIndex( jx, jy )];
					 }
				 }
			 }
		 }
		 float factor = 1f / ( ( hi - lo ) * ( hi - lo ) );
		 for ( int i = 0; i < out.length; i++ ) {
			 out[ i ] *= factor;
		 }
		 return out;
	 }

	 /**
	  * Returns a leveller for a given data grid and contour style.
	  *
	  * @param   array  grid data
	  * @param  style  contour style
	  * @return  leveller object
	  */
	 private static Leveller createLeveller( double[] array,
			 ContourStyle style ) {
		 final double[] levels = style.getLevelMode()
				 .calculateLevels( array, style.getLevelCount(),
						 style.getOffset(), true );
		 return new Leveller() {
			 @Override
			 public int getLevel( double count ) {
				 int ipos = Arrays.binarySearch( levels, count );
				 return ipos < 0 ? - ( ipos + 1 ) : ipos;
			 }
		 };
	 }

	 /**
	  * Knows how to turn a pixel value into an integer level.
	  */
	 private interface Leveller {

		 /**
		  * Returns the level value for a given pixel value.
		  *
		  * @param  pixel value
		  * @return  contour level
		  */
		 int getLevel( double count );
	 }
}
