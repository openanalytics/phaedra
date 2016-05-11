package eu.openanalytics.phaedra.ui.wellimage.tooltip;

import java.io.IOException;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.ui.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellToolTipLabelProvider extends ToolTipLabelProvider {

	protected static final String PREFERENCE_PAGE = Activator.PLUGIN_ID + ".ImageToolTipPreferencePage";

	private Resource disposeResource;

	private boolean[] channels;

	public WellToolTipLabelProvider() {
		// Do nothing.
	}

	@Override
	public Image getImage(Object element) {
		boolean showImage = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SHOW_IMAGE_TOOLTIP);
		if (showImage && element instanceof Well) {
			Well well = (Well) element;
			ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(well);
			if (channels == null || channels.length != currentSettings.getImageChannels().size()) {
				channels = new boolean[currentSettings.getImageChannels().size()];
				for (int i = 0; i < channels.length; i++) {
					channels[i] = currentSettings.getImageChannels().get(i).isShowInWellView();
				}
			}
			float scale = Activator.getDefault().getPreferenceStore().getFloat(Prefs.WELL_IMAGE_TOOLTIP_SCALE);

			try {
				ImageData imgData = ImageRenderService.getInstance().getWellImageData(well, limitScale(well, scale), channels);
				Image img = new Image(Display.getDefault(), imgData);
				markForDispose(img);
				return img;
			} catch (IOException e) {
				Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, "Failed to load image"));
			}
		}

		return null;
	}

	@Override
	public String getText(Object element) {
		boolean showText = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SHOW_TEXT_TOOLTIP);
		if (showText && element instanceof Well) {
			Well well = (Well) element;
			return "Well: " + NumberUtils.getWellCoordinate(well.getRow(), well.getColumn())
					+ "\nPlate: " + well.getPlate().getBarcode()
					+ "\nExperiment " + well.getPlate().getExperiment().getName();
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		markForDispose(null);
	}

	@Override
	public boolean hasAdvancedControls() {
		return Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SHOW_ADVANCED_TOOLTIP);
	}

	@Override
	public void fillAdvancedControls(Composite parent, Object element, final IToolTipUpdate update) {
		Link prefPage = new Link(parent, SWT.NONE);
		prefPage.setText("<a>Pref. Page</a>");
		prefPage.setBackground(parent.getBackground());
		prefPage.addListener(SWT.Selection, e -> {
			parent.getShell().close();
			PreferencesUtil.createPreferenceDialogOn(null, PREFERENCE_PAGE, null, null).open();
		});
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(prefPage);
		if (element instanceof Well) {
			ImageControlPanel imgControl = new ImageControlPanel(parent, SWT.BORDER, false, false);
			imgControl.setImage((Well) element);
			imgControl.setButtonStates(channels);
			imgControl.addImageControlListener(new ImageControlListener() {
				@Override
				public void componentToggled(int component, boolean state) {
					channels[component] = state;
					update.execute();
				}
			});
			GridDataFactory.fillDefaults().grab(true, true).applyTo(imgControl);
		}
	}


	/**
	 * Dispose the previous Resource and keep a reference to the current one so it can be disposed when needed.
	 *
	 * @param resource <code>null</code> to just dispose the previous resource
	 */
	protected void markForDispose(Resource resource) {
		if (disposeResource != null) disposeResource.dispose();
		this.disposeResource = resource;
	}

	/**
	 * Reduce the scale so that the tooltip image is no larger than the limits given in the preferences
	 * IMAGE_TOOLTIP_MAX_X , IMAGE_TOOLTIP_MAX_Y.
	 *
	 * @param well The well whose image will be rendered.
	 * @param scale The original scale to apply to the tooltip image.
	 * @return A possibly reduced scale.
	 */
	protected float limitScale(Well well, float scale) {
		int maxX = Activator.getDefault().getPreferenceStore().getInt(Prefs.IMAGE_TOOLTIP_MAX_X);
		int maxY = Activator.getDefault().getPreferenceStore().getInt(Prefs.IMAGE_TOOLTIP_MAX_Y);

		float limitedScale = scale;
		Point size = ImageRenderService.getInstance().getWellImageSize(well, limitedScale);
		while (size.x > maxX || size.y > maxY) {
			limitedScale /= 2.0;
			size = ImageRenderService.getInstance().getWellImageSize(well, limitedScale);
		}
		return limitedScale;
	}
}
