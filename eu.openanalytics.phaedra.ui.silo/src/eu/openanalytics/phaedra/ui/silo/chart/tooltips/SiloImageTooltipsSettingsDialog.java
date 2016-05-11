package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import static eu.openanalytics.phaedra.ui.silo.chart.tooltips.SiloImageTooltipProvider.CFG_CHANNELS;
import static eu.openanalytics.phaedra.ui.silo.chart.tooltips.SiloImageTooltipProvider.CFG_SCALE;
import static eu.openanalytics.phaedra.ui.silo.chart.tooltips.SiloImageTooltipProvider.DEFAULT_CHANNELS;
import static eu.openanalytics.phaedra.ui.silo.chart.tooltips.SiloImageTooltipProvider.DEFAULT_SCALE;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;

public class SiloImageTooltipsSettingsDialog extends TooltipSettingsDialog<Silo, Silo> {

	private ImageControlPanel imgControlPanel;

	private float scale;
	private boolean[] channels;

	public SiloImageTooltipsSettingsDialog(Shell parentShell, AbstractChartLayer<Silo, Silo> layer) {
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
		Silo key = getLayer().getDataProvider().getKey(0);
		imgControlPanel.setImage(key.getProtocolClass());
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