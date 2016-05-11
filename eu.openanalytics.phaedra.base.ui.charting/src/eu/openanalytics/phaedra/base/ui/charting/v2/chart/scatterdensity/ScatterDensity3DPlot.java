package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour.Gridder;
import uk.ac.starlink.ttools.plot.BinGrid;
import uk.ac.starlink.ttools.plot.BitmapSortPlotVolume;
import uk.ac.starlink.ttools.plot.CartesianPlot3D;
import uk.ac.starlink.ttools.plot.DataColorTweaker;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.ShaderTweaker;

public class ScatterDensity3DPlot extends CartesianPlot3D {

	private static final long serialVersionUID = 4588102509893848942L;

	private BinGrid[] grids_;

	private int nPotential_;
	private int nVisible_;

	private BufferedImage image_;

	private Rectangle lastPlotZone_;
	private ScatterDensity3DPlotState lastState_;

	private double lastZoom_;
	private double[] lastRotation_;

	public ScatterDensity3DPlot() {
		super();
		plotArea_.setOpaque(false);
	}

	@Override
	protected void drawData(Graphics2D g, Component c) {
		Rectangle plotZone = getPlotBounds();
		if ( plotZone.isEmpty()) {
			return;
		}
		ScatterDensity3DPlotState state = (ScatterDensity3DPlotState) getState();
		if ( state == null || ! state.getValid() ) {
			return;
		}
		PlotData data = state.getPlotData();
		if ( data == null ) {
			return;
		}
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
		if (getSelectionBitSet() != null && getSelectionBitSet().cardinality() != 0) {
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

		// Do not calculate or draw the contours during rotation for performance reasons.
		if (!state.getRotating()) {
			// Keep previous color and clipping.
			Shape clip = g.getClip();
			Color color = g.getColor();

			BufferedImage im = getImage(plotZone, state, styles, opacity);
			g.setClip(plotZone);
			g.drawImage(im, plotZone.x, plotZone.y, null);

			// Restore previous color and clipping.
			g.setClip(clip);
			g.setColor(color);
		}

		plotAxes(state, g, lastTrans_, lastVol_, true);

		firePlotChangedLater(new PlotEvent(this, state, nPotential_, getSelectionBitSet() != null ? getSelectionBitSet().cardinality() : 0, nVisible_));
	}

	private BufferedImage getImage(Rectangle plotZone, ScatterDensity3DPlotState state, MarkStyle[] styles, float opacity) {

		boolean forceUpdate = state.getZoomScale() != lastZoom_ || !state.getRotation().equals(lastRotation_);
		if (image_ == null || forceUpdate || !plotZone.equals(lastPlotZone_) || !state.equals(lastState_)) {

			boolean weighted = state.getWeighted();
			double loCut = state.getLoCut();
			double hiCut = state.getHiCut();

			int xsize = plotZone.width;
			int ysize = plotZone.height;
			BufferedImage image = new BufferedImage(xsize, ysize, BufferedImage.TYPE_INT_ARGB);

			// Note: always bin using pixelSize = 1, or the scatter coordinates will be off!
			doBinning(plotZone, 1, forceUpdate);
			BinGrid[] grids = grids_;

			for (int is = 0; is < grids.length; is++) {
				BinGrid grid = grids[is];
				if (grid != null) {
					double lo = grid.getCut(loCut);
					double hi = grid.getCut(hiCut);
					if (lo == 1 && !weighted) {
						lo = 0;
					}
					short[] bdata = grid.getBytes(lo, hi, state.getLogZ());
					double[] dataArrays = new double[bdata.length];
					for (int i=0; i<bdata.length; i++) {
						dataArrays[i] = (bdata[i] == Short.MAX_VALUE) ? 0 : bdata[i];
					}

					MarkShape shape = MarkShape.FILLED_CIRCLE;
					Pixellator pixellator = shape.getStyle(Color.BLACK, state.getPixelSize(), 1.0f).getPixelOffsets();
					Gridder gridder = new Gridder(xsize, ysize);
					int[] convolved = convolve(dataArrays, gridder, pixellator);

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
						int x = i%xsize;
						int y = ysize - (i/xsize) - 1;
						raster.setSample(x, y, 0, red[scaledValue]);
						raster.setSample(x, y, 1, green[scaledValue]);
						raster.setSample(x, y, 2, blue[scaledValue]);
						raster.setSample(x, y, 3, 255);
					}

				}
			}

			image_ = image;
			lastPlotZone_ = plotZone;
			lastState_ = state;
			lastZoom_ = state.getZoomScale();
			lastRotation_ = state.getRotation();
		}

		return image_;
	}

	private void doBinning(Rectangle zone, int pixsize, boolean forceUpdate) {

		/* Work out the data space bounds of the required grid. */
		ScatterDensity3DPlotState state = (ScatterDensity3DPlotState) getState();

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
