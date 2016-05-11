package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import uk.ac.starlink.ttools.plot.MarkStyle;

public class Scatter2DLegendItem<ENTITY, ITEM> extends AbstractLegendItem<ENTITY, ITEM> {

	private int size = 3;
	private float opacity = 1.0f;

	public Scatter2DLegendItem(AbstractLegend<ENTITY, ITEM> parent, String name, boolean enabled, boolean auxilaryData) {
		super(parent, name, enabled, auxilaryData);

		if (getStyle() instanceof MarkStyle) {
			MarkStyle markStyle = (MarkStyle) getStyle();
			this.size = markStyle.getSize();
			this.opacity = markStyle.getOpacity();
		}
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
		if (getStyle() instanceof MarkStyle) {
			((MarkStyle) getStyle()).setSize(size);
			// Also change the default size if only one available
			ChartSettings chartSettings = getParent().getChartSettings();
			if (getParent().getDataProvider().getActiveGroupingStrategy().getGroupCount() == 1) {
				chartSettings.setDefaultSymbolSize(size);
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (getStyle() instanceof MarkStyle) {
			//getParent().getChartSettings().getStyle(getName()).setHidePoints(enabled);
			((MarkStyle) getStyle()).setHidePoints(enabled);
		}
		super.setEnabled(enabled);
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
		if (getStyle() != null) {
			((MarkStyle) getStyle()).setOpacity(opacity);
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
		return super.canModify(property) || SIZE.equals(property) || OPACITY.equals(property);
	}

	@Override
	public String getPropertyValue(String property) {
		if (SIZE.equals(property)) {
			return String.valueOf(getSize());
		} else if (OPACITY.equals(property)) {
			return String.valueOf(getOpacity());
		}
		return super.getPropertyValue(property);
	}

	@Override
	public void setPropertyValue(String property, String value) {
		if (SIZE.equals(property)) {
			Pattern p = Pattern.compile("[0-9]|1[0-9]|20");
			Matcher m = p.matcher(value);
			if (m.matches()) {
				setSize(Integer.parseInt(value));
			}
		} else if (OPACITY.equals(property)) {
			if (NumberUtils.isDouble(value)) {
				float opacity = Float.parseFloat(value);
				if (opacity > 1) opacity = 1;
				if (opacity < 0) opacity = 0;
				setOpacity(opacity);
			}
		}
	}

}