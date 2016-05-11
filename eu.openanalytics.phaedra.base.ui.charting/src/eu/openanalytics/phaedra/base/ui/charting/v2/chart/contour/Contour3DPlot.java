package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import java.awt.Color;
import java.awt.Component;
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

import uk.ac.starlink.ttools.plot.BinGrid;
import uk.ac.starlink.ttools.plot.BitmapSortPlotVolume;
import uk.ac.starlink.ttools.plot.CartesianPlot3D;
import uk.ac.starlink.ttools.plot.DataColorTweaker;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.ShaderTweaker;
import uk.ac.starlink.ttools.plot.Style;

public class Contour3DPlot extends CartesianPlot3D {

	private static final long serialVersionUID = 5362765340817273177L;

	private BinGrid[] grids_;

	private int nPotential_;
	private int nVisible_;

	private BufferedImage image_;
	private BitSet selectionBitSet_;

	private Rectangle lastPlotZone_;
	private Contour3DPlotState lastState_;

	private BufferedImage selectionImage_;
	private Leveller leveller_;
	private double[] dataArrays_;
	private Gridder gridder_;
	private BitSet selLvls_;
	private boolean isSelectionValid_;
	private double lastZoom_;
	private double[] lastRotation_;

	public Contour3DPlot() {
		super();
		plotArea_.setOpaque(false);
		selLvls_ = new BitSet();
	}

	@Override
	protected void drawData(Graphics2D g, Component c) {
		Contour3DPlotState state = (Contour3DPlotState) getState();
		if ( state == null || ! state.getValid() ) return;
		Rectangle plotZone = getPlotBounds();
		if ( plotZone.isEmpty()) return;
		PlotData data = state.getPlotData();
		if ( data == null ) return;

		BufferedImage im = null;
		// Do not calculate or draw the contours during rotation for performance reasons.
		if (!state.getRotating()) {
			int nset = data.getSetCount();

			/* Set up a transformer to do the mapping from data space to
			 * Normalized 3-d view space. */
			lastTrans_ = new Transformer3D( state.getRotation(), loBoundsG_, hiBoundsG_, state.getZoomScale() );

			/* Work out padding factors for the plot volume. */
			int[] padBorders = new int[ 4 ];
			double padFactor = getPadding( state, null, padBorders );

			/* Prepare an array of styles which we may need to plot on the
			 * PlotVolume object. */
			float opacity = 1f;
			MarkStyle[] styles = new MarkStyle[ nset + 1 ];
			for ( int is = 0; is < nset; is++ ) {
				styles[ is ] = (MarkStyle) data.getSetStyle( is );
				opacity = 1 - styles[is].getOpacity();
			}
			styles[ nset ] = DOT_STYLE;

			// See if there is a selection.
			boolean hasSelection = false;
			if (selectionBitSet_ != null && selectionBitSet_.cardinality() != 0) {
				hasSelection = true;
			}

			/* See if there are any error bars to be plotted.  If not, we can
			 * use more efficient rendering machinery. */
			boolean anyErrors = false;
			for ( int is = 0; is < nset && ! anyErrors; is++ ) {
				anyErrors = anyErrors || MarkStyle.hasErrors( styles[ is ], data );
			}

			/* See if there may be labels to draw. */
			boolean hasLabels = data.hasLabels();

			/* Get fogginess. */
			double fog = state.getFogginess();

			zmax_ = Double.MAX_VALUE;

			/* Work out how points will be shaded according to auxiliary axis
			 * coordinates.  This does not include the effect of fogging,
			 * which will be handled separately. */
			DataColorTweaker tweaker = ShaderTweaker.createTweaker( 3, state );

			lastVol_ = new BitmapSortPlotVolume( c, g, styles, padFactor,
					padBorders, fog, hasLabels,
					anyErrors, hasSelection, -1.0, 2.0, tweaker,
					getBitmapSortWorkspace() );

			plotAxes(state, g, lastTrans_, lastVol_, false);

			int psize = state.getPixelSize();

			im = getImage(plotZone, state, styles, opacity);

			BufferedImageOp scaleOp = new AffineTransformOp(AffineTransform.getScaleInstance(psize, psize), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			g.setClip(plotZone);
			g.drawImage(im, scaleOp, plotZone.x, plotZone.y);

			plotAxes(state, g, lastTrans_, lastVol_, true);
		} else {
			setPlotTime_(nPotential_/ 100);
			super.setSelectionBitSet(selectionBitSet_);
			super.drawData(g, c);
		}

		firePlotChangedLater(new PlotEvent(this, state, nPotential_, selectionBitSet_ != null ? selectionBitSet_.cardinality() : 0, nVisible_));
	}

	private BufferedImage getImage(Rectangle plotZone, Contour3DPlotState state, MarkStyle[] styles, float opacity) {

		boolean forceUpdate = state.getZoomScale() != lastZoom_ || !state.getRotation().equals(lastRotation_);
		if (image_ == null || forceUpdate || !plotZone.equals(lastPlotZone_) || !state.equals(lastState_)) {

			boolean weighted = state.getWeighted();
			int xsize = plotZone.width;
			int ysize = plotZone.height;
			int psize = state.getPixelSize();
			int xpix = (xsize + psize - 1) / psize;
			int ypix = (ysize + psize - 1) / psize;

			BufferedImage image = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();

			doBinning(plotZone, psize, forceUpdate);
			BinGrid[] grids = grids_;

			double loCut = state.getLoCut();
			double hiCut = state.getHiCut();

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
								g.fillRect(ix, ypix - iy - 1, 1, 1 );
							}
							lev0 = lev1;
						}
					}
					for ( int iy = 0; iy < ypix; iy++ ) {
						int lev0 = leveller_.getLevel(dataArrays_[gridder_.getIndex( 0, iy )]);
						for ( int ix = 1; ix < xpix; ix++ ) {
							int lev1 = leveller_.getLevel(dataArrays_[gridder_.getIndex( ix, iy )]);
							if ( lev1 != lev0 ) {
								g.fillRect(ix - 1, ypix - iy - 1, 1, 1 );
							}
							lev0 = lev1;
						}
					}
				}
			}

			image_ = image;
			lastPlotZone_ = plotZone;
			lastState_ = state;
			lastZoom_ = state.getZoomScale();
			lastRotation_ = state.getRotation();
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
							g.fillRect(ix, ypix - iy - 1, 1, 1 );
						}
					}
				}

				updateSelection();
			}

			BufferedImage img = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_ARGB);
			Graphics g = img.getGraphics();
			g.drawImage(image_, 0, 0, null);
			g.drawImage(selectionImage_, 0, 0, null);
			return img;
		}

		return image_;
	}

	@Override
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
			//				int x = xy.x;
			//				int y = lastPlotZone_.height - xy.y;
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
					int y = (lastPlotZone_.height - (iy - lastPlotZone_.y)) / psize;
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

				if ( use ) {
					Point xy = placer.getXY( pseq.getPoint() );
					if ( xy != null ) {
						int x = xy.x / psize;
						int y = (lastPlotZone_.height - xy.y) / psize;
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
		}
		pseq.close();
		pseq = null;

		return selectionBitSet_;
	}

	/**
	 * Update the selectionBitSet to match the changed level layout.
	 */
	private void updateSelection() {
		selectionBitSet_.clear();

		int psize = lastState_.getPixelSize();

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

				if ( use ) {
					Point xy = placer.getXY( pseq.getPoint() );
					if ( xy != null ) {
						int x = xy.x / psize;
						int y = (lastPlotZone_.height - xy.y) / psize;
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
		}
		pseq.close();
		pseq = null;
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

	private void doBinning(Rectangle zone, int pixsize, boolean forceUpdate) {

		/* Work out the data space bounds of the required grid. */
		Contour3DPlotState state = (Contour3DPlotState) getState();

		/*
		 * See if we already have BinGrids with the right characteristics. If
		 * not, we have to calculate one.
		 */
		int xsize = zone.width;
		int ysize = zone.height;
		int xpix = (xsize + pixsize - 1) / pixsize;
		int ypix = (ysize + pixsize - 1) / pixsize;

		if ( forceUpdate || grids_ == null || grids_.length == 0 || grids_[0].getSizeX() != xpix || grids_[0].getSizeY() != ypix
				|| !state.equals(lastState_)) {

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

			PointPlacer placer = getPointPlacer();
			boolean[] setFlags = new boolean[nset];
			int nVisible = 0;
			PointSequence pseq = data.getPointSequence();
			int ip = 0;
			for (; pseq.next(); ip++) {
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
							&& !Double.isInfinite(coords[2])) {
						Point xy = placer.getXY(coords);
						if (xy != null) {
							int ix = xy.x / pixsize;
							int iy = xy.y / pixsize;
							if (ix >= 0 && ix < xpix && iy >= 0 && iy < ypix) {
								nVisible++;
								for (int is = 0; is < nset; is++) {
									if (setFlags[is]) {
										grids[sumAll ? 0 : is].submitDatum(ix, iy, coords[3]);
									}
								}
							}
						}
					}
				}
			}
			pseq.close();
			grids_ = grids;
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
