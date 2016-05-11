package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import org.eclipse.swt.graphics.RGB;

public interface IContourPlotState {

	public abstract void setRgb(boolean rgb);

	public abstract boolean getRgb();

	public abstract void setLogZ(boolean zLog);

	public abstract boolean getLogZ();

	public abstract void setPixelSize(int psize);

	public abstract int getPixelSize();

	public abstract void setLoCut(double frac);

	public abstract double getLoCut();

	public abstract void setHiCut(double frac);

	public abstract double getHiCut();

	public abstract void setWeighted(boolean weighted);

	public abstract boolean getWeighted();

	public abstract RGB getColor();

	public abstract void setColor(RGB color);

	public abstract int getLevels();

	public abstract void setLevels(int levels);

	public abstract double getOffset();

	public abstract void setOffset(double offset);

	public abstract int getSmooth();

	public abstract void setSmooth(int smooth);

	public abstract String getLevelMode();

	public abstract void setLevelMode(String levelMode);

}