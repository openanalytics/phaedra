package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import org.eclipse.swt.graphics.RGB;

import uk.ac.starlink.ttools.plot.PlotState;

public class ContourPlotState extends PlotState implements IContourPlotState {

	private boolean rgb_;
	private boolean zLog_;
    private double loCut_;
    private double hiCut_;
    private int pixSize_;
    private boolean weighted_;

    private RGB color;
    private int levels;
    private double offset;
    private int smooth;
    private String levelMode;

    @Override
	public void setRgb( boolean rgb ) {
        rgb_ = rgb;
    }

    @Override
	public boolean getRgb() {
        return rgb_;
    }

    @Override
	public void setLogZ( boolean zLog ) {
        zLog_ = zLog;
    }

    @Override
	public boolean getLogZ() {
        return zLog_;
    }

    @Override
	public void setPixelSize( int psize ) {
        pixSize_ = psize;
    }

    @Override
	public int getPixelSize() {
        return pixSize_;
    }

    @Override
	public void setLoCut( double frac ) {
        loCut_ = frac;
    }

    @Override
	public double getLoCut() {
        return loCut_;
    }

    @Override
	public void setHiCut( double frac ) {
        hiCut_ = frac;
    }

    @Override
	public double getHiCut() {
        return hiCut_;
    }

    @Override
	public void setWeighted( boolean weighted ) {
        weighted_ = weighted;
    }

    @Override
	public boolean getWeighted() {
        return weighted_;
    }

	@Override
	public RGB getColor() {
		return color;
	}

	@Override
	public void setColor(RGB color) {
		this.color = color;
	}

	@Override
	public int getLevels() {
		return levels;
	}

	@Override
	public void setLevels(int levels) {
		this.levels = levels;
	}

	@Override
	public double getOffset() {
		return offset;
	}

	@Override
	public void setOffset(double offset) {
		this.offset = offset;
	}

	@Override
	public int getSmooth() {
		return smooth;
	}

	@Override
	public void setSmooth(int smooth) {
		this.smooth = smooth;
	}

	@Override
	public String getLevelMode() {
		return levelMode;
	}

	@Override
	public void setLevelMode(String levelMode) {
		this.levelMode = levelMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		long temp;
		temp = Double.doubleToLongBits(hiCut_);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((levelMode == null) ? 0 : levelMode.hashCode());
		result = prime * result + levels;
		temp = Double.doubleToLongBits(loCut_);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(offset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + pixSize_;
		result = prime * result + (rgb_ ? 1231 : 1237);
		result = prime * result + smooth;
		result = prime * result + (weighted_ ? 1231 : 1237);
		result = prime * result + (zLog_ ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContourPlotState other = (ContourPlotState) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (Double.doubleToLongBits(hiCut_) != Double.doubleToLongBits(other.hiCut_))
			return false;
		if (levelMode == null) {
			if (other.levelMode != null)
				return false;
		} else if (!levelMode.equals(other.levelMode))
			return false;
		if (levels != other.levels)
			return false;
		if (Double.doubleToLongBits(loCut_) != Double.doubleToLongBits(other.loCut_))
			return false;
		if (Double.doubleToLongBits(offset) != Double.doubleToLongBits(other.offset))
			return false;
		if (pixSize_ != other.pixSize_)
			return false;
		if (rgb_ != other.rgb_)
			return false;
		if (smooth != other.smooth)
			return false;
		if (weighted_ != other.weighted_)
			return false;
		if (zLog_ != other.zLog_)
			return false;
		return true;
	}

}
