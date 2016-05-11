package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import uk.ac.starlink.ttools.plot.Plot3DState;
import uk.ac.starlink.ttools.plot.Shader;

/**
 * PlotState specialist subclass used for density maps.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class ScatterDensity3DPlotState extends Plot3DState {

    private boolean rgb_;
    private boolean zLog_;
    private double loCut_;
    private double hiCut_;
    private float gaussianFilter;
    private boolean skipZeroDensity;
    private int pixSize_;
    private boolean weighted_;
    private Shader indexedShader_;

    /**
     * Sets whether the plot will be coloured.
     *
     * @param   rgb  true for colour, false for monochrome
     */
    public void setRgb( boolean rgb ) {
        rgb_ = rgb;
    }

    /**
     * Determines whether the plot will be coloured.
     *
     * @return  true for colour, false for monochrome
     */
    public boolean getRgb() {
        return rgb_;
    }

    /**
     * Sets whether the colour intensity is to be plotted on a log or
     * linear scale.
     *
     * @param  zLog  true iff you want logarithmic scaling of intensity
     */
    public void setLogZ( boolean zLog ) {
        zLog_ = zLog;
    }

    /**
     * Determines whether the colour intensity is to be plotted on a log or
     * linear scale.
     *
     * @return  true iff scaling will be logarithmic
     */
    public boolean getLogZ() {
        return zLog_;
    }

    /**
     * Sets the size of each data pixel (bin) in screen pixels.
     *
     * @param  psize  pixel size
     */
    public void setPixelSize( int psize ) {
        pixSize_ = psize;
    }

    /**
     * Gets the size of each data pixel (bin) in screen pixels.
     *
     * @return   pixel size
     */
    public int getPixelSize() {
        return pixSize_;
    }

    /**
     * Sets the lower cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @param  frac   lower cut value (0-1)
     */
    public void setLoCut( double frac ) {
        loCut_ = frac;
    }

    /**
     * Gets the lower cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @return  lower cut value (0-1)
     */
    public double getLoCut() {
        return loCut_;
    }

    /**
     * Sets the upper cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @param  frac   upper cut value (0-1)
     */
    public void setHiCut( double frac ) {
        hiCut_ = frac;
    }

    /**
     * Gets the upper cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @return  upper cut value (0-1)
     */
    public double getHiCut() {
        return hiCut_;
    }

    /**
     * Sets the gaussian filter (1 = no gaussian).
     *
     * @param gaussianFilter
     */
    public void setGaussianFilter(float gaussianFilter) {
		this.gaussianFilter = gaussianFilter;
	}

    /**
     * Gets the gaussian filter (should be between 1 and 4)
     *
     * @return gaussianFilter
     */
    public float getGaussianFilter() {
		return gaussianFilter;
	}

    /**
     * True if empty/zero values should not be drawn
     *
     * @param skipZeroDensity
     */
    public void setSkipZeroDensity(boolean skipZeroDensity) {
		this.skipZeroDensity = skipZeroDensity;
	}

    /**
     * Returns true if the empty/zero values should not be drawn
     * @return
     */
    public boolean isSkipZeroDensity() {
		return skipZeroDensity;
	}

    /**
     * Sets whether non-unit weighting is (maybe) in force for this state.
     *
     * @param  weighted  whether weights are used
     */
    public void setWeighted( boolean weighted ) {
        weighted_ = weighted;
    }

    /**
     * Determines whether non-unit weighting is (maybe) in force for this state.
     *
     * @return  whether weights are used
     */
    public boolean getWeighted() {
        return weighted_;
    }

    /**
     * Sets the shader object to be used for shading pixels in
     * indexed (non-RGB) mode.
     *
     * @param  indexedShader  shader
     */
    public void setIndexedShader( Shader indexedShader ) {
        indexedShader_ = indexedShader;
    }

    /**
     * Returns the shader to be used for shading pixels in
     * indexed (non-RGB) mode.
     *
     * @return  shader
     */
    public Shader getIndexedShader() {
        return indexedShader_;
    }

    @Override
	public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof ScatterDensity3DPlotState ) {
            ScatterDensity3DPlotState other = (ScatterDensity3DPlotState) o;
            return rgb_ == other.rgb_
                && zLog_ == other.zLog_
                && loCut_ == other.loCut_
                && hiCut_ == other.hiCut_
                && pixSize_ == other.pixSize_
                && weighted_ == other.weighted_
                && indexedShader_ == other.indexedShader_
            	&& gaussianFilter == other.gaussianFilter;
        }
        else {
            return false;
        }
    }

    @Override
	public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + ( rgb_ ? 3 : 0 );
        code = 23 * code + ( zLog_ ? 5 : 0 );
        code = 23 * code + Float.floatToIntBits( (float) loCut_ );
        code = 23 * code + Float.floatToIntBits( (float) hiCut_ );
        code = 23 * code + pixSize_;
        code = 23 * code + ( weighted_ ? 7 : 0 );
        code = 23 * code + ( indexedShader_ == null
                                 ? 0
                                 : indexedShader_.hashCode() );
        return code;
    }
}
