package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.io.Serializable;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class AuxiliaryChartSettings implements Serializable {

	private static final long serialVersionUID = -1425057253923136151L;
	
	private String shader;
	
	private double loCut;
	private double hiCut;
	private int pixelSize;
	private float gauss;
	private float transparancy;
	private boolean skipZeroDensity;
	
	private String weightFeature;

	public AuxiliaryChartSettings() {
		this.shader = Shaders.LUT_RAINBOW.getName();
		this.loCut = 0.0;
		this.hiCut = 1.0;
		this.pixelSize = 2;
		this.gauss = 1.25f;
		this.transparancy = 0.0f;
		this.skipZeroDensity = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SKIP_ZERO_DENSITY);
	}
	
	public Shader getShader() {
		return Shaders.getShaderByName(shader);
	}

	public void setShader(Shader shader) {
		if (shader != null) {
			this.shader = shader.getName();
		} else {
			this.shader = Shaders.LUT_RAINBOW.getName();
		}
	}

	public double getLoCut() {
		return loCut;
	}

	public void setLoCut(double loCut) {
		this.loCut = loCut;
	}

	public double getHiCut() {
		return hiCut;
	}

	public void setHiCut(double hiCut) {
		this.hiCut = hiCut;
	}

	public String getWeightFeature() {
		return weightFeature;
	}

	public void setWeightFeature(String weightFeature) {
		this.weightFeature = weightFeature;
	}

	public int getPixelSize() {
		return pixelSize;
	}

	public void setPixelSize(int pixelSize) {
		this.pixelSize = pixelSize;
	}

	public float getGauss() {
		return gauss;
	}

	public void setGauss(float gauss) {
		this.gauss = gauss;
	}

	public float getTransparancy() {
		return transparancy;
	}

	public void setTransparancy(float transparancy) {
		this.transparancy = transparancy;
	}

	public boolean isSkipZeroDensity() {
		return skipZeroDensity;
	}
	
	public void setSkipZeroDensity(boolean skipZeroDensity) {
		this.skipZeroDensity = skipZeroDensity;
	}
	
}