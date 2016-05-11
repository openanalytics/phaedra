package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import java.util.Arrays;

public class SpatialConfig {

	private int[] modelSize;
	private int rasterSize;
	
	private float zoomFactor;
	
	private float[] cameraRotation;
	private float[] cameraPan;
	private float[] cameraOrbit;
	
	private float[] bgColor;
	private float[] rasterColor;
	
	private float pointOpacity;
	private float pointSize;

	private boolean planeBordersEnabled;
	private boolean rasteredPlanesEnabled;
	private boolean solidPlanesEnabled;
	private boolean zGradientEnabled;
	
	private boolean axesEnabled;
	private String[] axisLabels;
	private boolean flipY;
	private boolean perspectiveEnabled;
	
	private int currentCanvasHeight;
	
	public SpatialConfig() {
		modelSize = new int[]{100,100,100};
		rasterSize = 10;
		zoomFactor = 0.75f;
		cameraRotation = new float[]{0f, 1f, 0f};
		cameraPan = new float[]{0f,0f,0f};
		cameraOrbit = new float[]{0f,0f,0f};
		bgColor = new float[]{.8f, .8f, .8f};
		rasterColor = new float[]{0f, 0f, 0f};
		pointOpacity = 1f;
		pointSize = 1f;
		rasteredPlanesEnabled = true;
		planeBordersEnabled = true;
		solidPlanesEnabled = false;
		perspectiveEnabled = true;
		axesEnabled = true;
		axisLabels = new String[]{"X","Y","Z"};
		zGradientEnabled = false;
		flipY = false;
	}
	
	public SpatialConfig clone() {
		SpatialConfig clone = new SpatialConfig();
		clone.setAxesEnabled(axesEnabled);
		clone.setAxisLabels(Arrays.copyOf(axisLabels,axisLabels.length));
		clone.setBgColor(Arrays.copyOf(bgColor, bgColor.length));
		clone.setCameraOrbit(Arrays.copyOf(cameraOrbit, cameraOrbit.length));
		clone.setCameraPan(Arrays.copyOf(cameraPan, cameraPan.length));
		clone.setCameraRotation(Arrays.copyOf(cameraRotation, cameraRotation.length));
		clone.setPointOpacity(pointOpacity);
		clone.setPointSize(pointSize);
		clone.setModelSize(Arrays.copyOf(modelSize, modelSize.length));
		clone.setRasterColor(Arrays.copyOf(rasterColor, rasterColor.length));
		clone.setRasteredPlanesEnabled(rasteredPlanesEnabled);
		clone.setPlaneBordersEnabled(planeBordersEnabled);
		clone.setPerspectiveEnabled(perspectiveEnabled);
		clone.setRasterSize(rasterSize);
		clone.setSolidPlanesEnabled(solidPlanesEnabled);
		clone.setZoomFactor(zoomFactor);
		clone.setzGradientEnabled(zGradientEnabled);
		
		//the canvas height may not be cloned, this is unique!
		//clone.setCurrentCanvasHeight(currentCanvasHeight);
		return clone;
	}
	
	public int[] getModelSize() {
		return modelSize;
	}
	public void setModelSize(int[] modelSize) {
		this.modelSize = modelSize;
	}
	public int getRasterSize() {
		return rasterSize;
	}
	public void setRasterSize(int rasterSize) {
		this.rasterSize = rasterSize;
	}
	public float getZoomFactor() {
		return zoomFactor;
	}
	public void setZoomFactor(float zoomFactor) {
		this.zoomFactor = zoomFactor;
	}
	public float[] getCameraRotation() {
		return cameraRotation;
	}
	public void setCameraRotation(float[] cameraRotation) {
		this.cameraRotation = cameraRotation;
	}
	public float[] getCameraPan() {
		return cameraPan;
	}
	public void setCameraPan(float[] cameraPan) {
		this.cameraPan = cameraPan;
	}
	public float[] getCameraOrbit() {
		return cameraOrbit;
	}
	public void setCameraOrbit(float[] cameraOrbit) {
		this.cameraOrbit = cameraOrbit;
	}
	public float[] getBgColor() {
		return bgColor;
	}
	public void setBgColor(float[] bgColor) {
		this.bgColor = bgColor;
	}
	public float[] getRasterColor() {
		return rasterColor;
	}
	public void setRasterColor(float[] rasterColor) {
		this.rasterColor = rasterColor;
	}
	public float getPointOpacity() {
		return pointOpacity;
	}
	public void setPointOpacity(float pointOpacity) {
		this.pointOpacity = pointOpacity;
	}
	public float getPointSize() {
		return pointSize;
	}
	public void setPointSize(float pointSize) {
		this.pointSize = pointSize;
	}
	public boolean isRasteredPlanesEnabled() {
		return rasteredPlanesEnabled;
	}
	public void setRasteredPlanesEnabled(boolean rasteredPlanesEnabled) {
		this.rasteredPlanesEnabled = rasteredPlanesEnabled;
	}
	public void setPlaneBordersEnabled(boolean planeBordersEnabled) {
		this.planeBordersEnabled = planeBordersEnabled;
	}

	public boolean isPlaneBordersEnabled() {
		return planeBordersEnabled;
	}

	public boolean isSolidPlanesEnabled() {
		return solidPlanesEnabled;
	}
	public void setSolidPlanesEnabled(boolean solidPlanesEnabled) {
		this.solidPlanesEnabled = solidPlanesEnabled;
	}
	public boolean isAxesEnabled() {
		return axesEnabled;
	}
	public void setAxesEnabled(boolean axesEnabled) {
		this.axesEnabled = axesEnabled;
	}
	public String[] getAxisLabels() {
		return axisLabels;
	}
	public void setAxisLabels(String[] axisLabels) {
		this.axisLabels = axisLabels;
	}
	public void setPerspectiveEnabled(boolean perspectiveEnabled) {
		this.perspectiveEnabled = perspectiveEnabled;
	}

	public boolean isPerspectiveEnabled() {
		return perspectiveEnabled;
	}

	public void setzGradientEnabled(boolean zGradientEnabled) {
		this.zGradientEnabled = zGradientEnabled;
	}

	public boolean iszGradientEnabled() {
		return zGradientEnabled;
	}

	public void setFlipY(boolean flipY) {
		this.flipY = flipY;
	}

	public boolean isFlipY() {
		return flipY;
	}

	public void setCurrentCanvasHeight(int currentCanvasHeight) {
		this.currentCanvasHeight = currentCanvasHeight;
	}

	public int getCurrentCanvasHeight() {
		return currentCanvasHeight;
	}
}
