package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipProvider.CFG_CHANNELS;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipProvider.CFG_SCALE;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipProvider.DEFAULT_CHANNELS;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipProvider.DEFAULT_SCALE;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;

public class WellImageTooltipsSettingsDialog extends TooltipSettingsDialog<Plate, Well> {

	private ImageControlPanel imgControlPanel;

	private float scale;
	private boolean[] channels;

	public WellImageTooltipsSettingsDialog(Shell parentShell, AbstractChartLayer<Plate, Well> layer) {
		super(parentShell, layer);
		Object obj = getSettings().getTooltipSettings().getMiscSetting(CFG_SCALE);
		if (obj instanceof Float) {
			this.scale = (float) obj;
		} else {
			this.scale = DEFAULT_SCALE;
		}
		obj = getSettings().getTooltipSettings().getMiscSetting(CFG_CHANNELS);
		if (obj instanceof boolean[]) {
			this.channels = (boolean[]) obj;
		} else {
			this.channels = DEFAULT_CHANNELS;
		}
	}

	@Override
	public Control embedDialogArea(Composite area) {
		imgControlPanel = new ImageControlPanel(area, SWT.NONE, true, false);
		imgControlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void scaleChanged(float ratio) {
				getSettings().getTooltipSettings().setMiscSetting(CFG_SCALE, imgControlPanel.getCurrentScale());
				getLayer().settingsChanged();
			}
			@Override
			public void componentToggled(int component, boolean state) {
				getSettings().getTooltipSettings().setMiscSetting(CFG_CHANNELS, imgControlPanel.getButtonStates());
				getLayer().settingsChanged();
			}
		});
		// Use the first Well available.
		imgControlPanel.setImage(getLayer().getDataProvider().getKey(0));
		imgControlPanel.setCurrentScale(scale);
		if (channels != null && imgControlPanel.getButtonStates().length == channels.length) {
			imgControlPanel.setButtonStates(channels);
		}
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(imgControlPanel);

		return super.embedDialogArea(area);
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
		getSettings().getTooltipSettings().setMiscSetting(CFG_SCALE, scale);
		getSettings().getTooltipSettings().setMiscSetting(CFG_CHANNELS, channels);
		getLayer().settingsChanged();
	}

}