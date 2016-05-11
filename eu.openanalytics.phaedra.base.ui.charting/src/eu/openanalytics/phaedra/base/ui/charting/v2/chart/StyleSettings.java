package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.awt.Color;
import java.io.Serializable;

import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.BarStyle.Form;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;

public class StyleSettings implements Serializable {

	private static final long serialVersionUID = 5536778865239417335L;

	private String classname;
	private String name;
	private int size;
	private Color color;
	private float opacity;
	private boolean visible;

	private void initialize(String name, int size, Color color, float opacity, boolean visible) {
		this.name = name;
		this.size = size;
		this.color = color;
		this.opacity = opacity;
		this.visible = visible;
	}

	public StyleSettings(Style style) {
		super();
		this.classname = style.getClass().getName();
		if (style instanceof MarkStyle) {
			MarkStyle s = (MarkStyle) style;
			initialize(s.getShapeId().toString(), s.getSize(), s.getColor(), s.getOpacity(), s.getHidePoints());
		}
		if (style instanceof DensityStyle) {
			DensityStyle s = (DensityStyle) style;
			initialize(s.getShader().getName(), 0, null, s.getOpacity(), true);
		}
		if (style instanceof BarStyle) {
			BarStyle s = (BarStyle) style;
			initialize(s.getForm().toString(), 0, s.getColor(), s.getOpacity(), s.getHidePoints());
		}
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	public float getOpacity() {
		return opacity;
	}
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public Style createStyle() {
		if (classname == null) {
			throw new RuntimeException("Cannot create style, classname not serialized.");
		}
		if (classname.equals(DensityStyle.class.getName())) {
			DensityStyle style = new DensityStyle(DensityStyle.GREEN) {
				@Override
				protected boolean isRGB() {
					return false;
				}
			};
			style.setOpacity(getOpacity());
			style.setShader(Shaders.getShaderByName(name));
			return style;
		} else if (classname.equals(BarStyle.class.getName())) {
			Form form = DefaultStyleProvider.getBarFormByString(getName());
			BarStyle style = new BarStyle(getColor(), form, BarStyle.PLACE_OVER);
			style.setOpacity(getOpacity());
			style.setHidePoints(isVisible());
		}
		MarkShape shape = DefaultStyleProvider.getMarkShapeByString(getName());
		MarkStyle style = shape.getStyle(getColor(), getSize(), getOpacity());
		style.setHidePoints(isVisible());
		return style;
	}

}