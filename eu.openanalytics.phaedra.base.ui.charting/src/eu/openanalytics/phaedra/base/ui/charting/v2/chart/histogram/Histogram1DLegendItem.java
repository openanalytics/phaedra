package eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.Style;

public class Histogram1DLegendItem<ENTITY, ITEM> extends AbstractLegendItem<ENTITY, ITEM> {

	private float opacity = 1.0f;

	public Histogram1DLegendItem(AbstractLegend<ENTITY, ITEM> parent, String name, boolean enabled, boolean auxilaryData) {
		super(parent, name, enabled, auxilaryData);

		Style style = getStyle();
		if (style != null) {
			this.opacity = style.getOpacity();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (getStyle() instanceof BarStyle) {
			((BarStyle) getStyle()).setHidePoints(enabled);
		}
		super.setEnabled(enabled);
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
		if (getStyle() instanceof BarStyle) {
			((BarStyle) getStyle()).setOpacity(opacity);
		}
	}

	@Override
	public Image createIconImage() {
		if (!hasAuxilaryData() && getStyle() != null) {
			Icon icon = getStyle().getLegendIcon();
			if (icon instanceof ImageIcon) {
				return AWTImageConverter.convert(Display.getCurrent(), ((ImageIcon) icon).getImage());
			} else {
				final BufferedImage image = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
				Graphics g = image.createGraphics();
				getStyle().getLegendIcon().paintIcon(null, g, 0, 0);
				g.dispose();
				return AWTImageConverter.convert(Display.getCurrent(), image);
			}
		}
		return null;
	}

	@Override
	public boolean canModify(String property) {
		return super.canModify(property) || OPACITY.equals(property);
	}

	@Override
	public String getPropertyValue(String property) {
		if (OPACITY.equals(property)) {
			return String.valueOf(getOpacity());
		}
		return super.getPropertyValue(property);
	}

	@Override
	public void setPropertyValue(String property, String value) {
		if (OPACITY.equals(property)) {
			if (NumberUtils.isDouble(value)) {
				float opacity = Float.parseFloat(value);
				if (opacity > 1) opacity = 1;
				if (opacity < 0) opacity = 0;
				setOpacity(opacity);
			}
		}
	}

}