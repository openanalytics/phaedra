package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.ConcurrentTaskResult;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas.JP2KImageCanvasListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class ImageLayer extends PlatesLayer {

	private static final String IMAGE_CHANNEL_ = "IMAGE_CHANNEL_";
	private static final String IMAGE_REGION = "IMAGE_REGION";
	private static final String IMAGE_SCALE = "IMAGE_SCALE";

	private WellImageRenderer renderer;
	private IUIEventListener uiEventListener;

	private boolean[] enabledComponents;
	private float scale;
	private Rectangle region;

	private int lastW;
	private int lastH;

	@Override
	public String getName() {
		return "Image";
	}

	@Override
	protected void doInitialize() {
		// Load settings
		long protocolId = ((Protocol) getPlate().getAdapter(Protocol.class)).getId();
		List<ImageChannel> channels = ImageSettingsService.getInstance().getCurrentSettings(getPlate()).getImageChannels();
		if (enabledComponents == null || enabledComponents.length != channels.size()) {
			enabledComponents = new boolean[channels.size()];
			int index = 0;
			for (ImageChannel channel : channels) {
				Boolean isActivated = GridState.getBooleanValue(protocolId, getId(), IMAGE_CHANNEL_ + index);
				if (isActivated == null) isActivated = channel.isShowInWellView();
				enabledComponents[index++] = isActivated;
			}
		}
		Object o = GridState.getValue(protocolId, getId(), IMAGE_REGION);
		if (o != null) region = (Rectangle) o;
		o = GridState.getValue(protocolId, getId(), IMAGE_SCALE);
		if (o != null) scale = (float) o;

		uiEventListener = event -> {
			if (event.type == EventType.ImageSettingsChanged && renderer != null) renderer.resetRendering();
		};
		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);
		
		if (renderer != null) renderer.resetRendering();
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new WellImageRenderer();
		return renderer;
	}

	@Override
	public boolean isRendering() {
		return renderer.isRendering();
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ConfigDialog(shell);
	}

	@Override
	public void dispose() {
		if (uiEventListener != null) ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
	}

	@Override
	public Object getConfig() {
		return enabledComponents;
	}

	@Override
	public void setConfig(Object config) {
		if (config instanceof boolean[]) {
			boolean[] tempEnabled = (boolean[]) config;
			if (enabledComponents == null) enabledComponents = new boolean[tempEnabled.length];
			for (int i = 0; i < enabledComponents.length && i < tempEnabled.length; i++) {
				enabledComponents[i] = tempEnabled[i];
			}
		}
	}

	private class WellImageRenderer extends BaseConcurrentGridCellRenderer {

		public WellImageRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}

		@Override
		public void prerender(Grid grid) {
			if (isEnabled()) super.prerender(grid);
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			lastW = w;
			lastH = h;
			if (isEnabled()) super.render(cell, gc, x, y, w, h);
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			return new WellImageRenderTask((Well) cell.getData(), w, h, cell.getRow(), cell.getColumn());
		}
	}

	private class WellImageRenderTask extends ConcurrentTask {

		private Well well;
		private int w, h;
		private int row, col;

		public WellImageRenderTask(Well well, int w, int h, int row, int col) {
			this.well = well;
			this.w = w;
			this.h = h;
			this.row = row;
			this.col = col;
		}

		@Override
		public void run() {
			ImageData data = null;
			try {
				if (region != null) {
					data = ImageRenderService.getInstance().getWellImageData(well, scale, region, enabledComponents);
				} else {
					data = ImageRenderService.getInstance().getWellImageData(well, w, h, enabledComponents);
				}
			} catch (IOException e) {
				// Render nothing.
			}
			setResult(new ConcurrentTaskResult(row, col, data));
		}
	}

	private class ConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

		private boolean[] newEnabledComponents;
		private boolean useCustomThumbnail;
		private float newScale;
		private Rectangle newRegion;

		private ImageControlPanel controlPanel;
		private JP2KImageCanvas imageCanvas;
		private Button useCustomThumbnailBtn;

		private Well sampleWell;
		private List<Well> availableWells;
		
		public ConfigDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configuration: " + getName());
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);

			newEnabledComponents = Arrays.copyOf(enabledComponents, enabledComponents.length);
			if (region != null) newRegion = new Rectangle(region.x, region.y, region.width, region.height);
			newScale = scale;
			useCustomThumbnail = (region != null);

			ProtocolClass pClass = (ProtocolClass) getPlate().getAdapter(ProtocolClass.class);
			List<ImageChannel> channels = pClass.getImageSettings().getImageChannels();

			availableWells = ProtocolService.streamableList(getWells(getCurrentInput()));
			Collections.sort(availableWells, PlateUtils.WELL_EXP_NAME_PLATE_BARCODE_WELL_NR_SORTER);
			
			// Image Channels
			// ------------------------

			Group channelGroup = new Group(container, SWT.NONE);
			channelGroup.setText("Image Channels");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(channelGroup);
			GridLayoutFactory.fillDefaults().numColumns(channels.size()).margins(5, 5).applyTo(channelGroup);

			controlPanel = new ImageControlPanel(channelGroup, SWT.NONE, false, false);
			controlPanel.addImageControlListener(new ImageControlListener() {
				@Override
				public void componentToggled(int component, boolean state) {
					newEnabledComponents[component] = state;
					imageCanvas.forceRedraw();
				}
			});

			// Image Offset
			// ------------------------

			Group offsetGroup = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(offsetGroup);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(offsetGroup);

			useCustomThumbnailBtn = new Button(offsetGroup, SWT.CHECK);
			useCustomThumbnailBtn.setText("Use a custom thumbnail:");
			useCustomThumbnailBtn.addListener(SWT.Selection, e -> useCustomThumbnail = useCustomThumbnailBtn.getSelection());
			useCustomThumbnailBtn.setSelection(useCustomThumbnail);

			imageCanvas = new JP2KImageCanvas(offsetGroup, SWT.NONE) {
				@Override
				protected boolean isChannelEnabled(int nr) {
					return !controlPanel.isDisabled(nr);
				}
			};
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(lastW + 3, lastH + 3).applyTo(imageCanvas);

			imageCanvas.addListener(new JP2KImageCanvasListener() {
				@Override
				public void onScaleChange(final float newScale) {
					ConfigDialog.this.newScale = newScale;
					if (newRegion == null) newRegion = new Rectangle(0, 0, 0, 0);
					newRegion.width = (int) Math.min(imageCanvas.getCurrentImageSize().x, lastW / newScale);
					newRegion.height = (int) Math.min(imageCanvas.getCurrentImageSize().y, lastH / newScale);
				}
				@Override
				public void onOffsetChange(final int x, final int y) {
					newRegion = new Rectangle(x, y
							, (int) Math.min(imageCanvas.getCurrentImageSize().x, lastW / newScale)
							, (int) Math.min(imageCanvas.getCurrentImageSize().y, lastH / newScale));
				}
				@Override
				public void onFileChange() {
					// Do nothing.
				}
			});

			// Control Buttons
			// ------------------------

			Composite btnComposite = new Composite(offsetGroup, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(btnComposite);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(btnComposite);

			new Label(btnComposite, SWT.NONE).setText("Use sample image from well:");
			final Combo sampleWellCombo = new Combo(btnComposite, SWT.READ_ONLY);
			sampleWellCombo.addListener(SWT.Selection, e -> {
				int index = sampleWellCombo.getSelectionIndex();
				loadSample(availableWells.get(index));
			});

			String[] items = new String[availableWells.size()];
			for (int i=0; i<items.length; i++) {
				Well well = availableWells.get(i);
				items[i] = PlateUtils.getWellCoordinate(well) + " (" + well.getPlate().getBarcode() + ")";
			}
			sampleWellCombo.setItems(items);

			Button resetBtn = new Button(btnComposite, SWT.PUSH);
			resetBtn.setText("Reset defaults");
			resetBtn.addListener(SWT.Selection, e -> {
				newRegion = null;
				calculateFitScale();
				imageCanvas.changeScale(newScale);
				imageCanvas.changeImageOffset(0, 0);
			});

			if (items.length > 0) {
				sampleWellCombo.select(0);
				loadSample(availableWells.get(0));
			}

			// Do this last to prevent NPE
			controlPanel.setButtonStates(newEnabledComponents);

			setTitle(getName());
			setMessage("Select the image channels to display on the grid.");
			return area;
		}

		@Override
		public void applySettings(GridViewer viewer, IGridLayer layer) {
			// Apply to the current layer
			ImageLayer imageLayer = (ImageLayer)layer;
			imageLayer.scale = newScale;
			if (!useCustomThumbnail) {
				imageLayer.region = null;
			} else {
				imageLayer.region = newRegion;
			}
			imageLayer.enabledComponents = newEnabledComponents;

			// And save for future layers
			long protocolId = ((Protocol) getPlate().getAdapter(Protocol.class)).getId();
			for (int i = 0; i < enabledComponents.length; i++) {
				GridState.saveValue(protocolId, getId(), IMAGE_CHANNEL_ + i, enabledComponents[i]);
			}
			GridState.saveValue(protocolId, getId(), IMAGE_REGION, region);
			GridState.saveValue(protocolId, getId(), IMAGE_SCALE, scale);
			redrawLayer(viewer);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			createButton(parent, IDialogConstants.OPEN_ID, "Apply", false);
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OPEN_ID) {
				applySettings(getLayerSupport().getViewer(), ImageLayer.this);
			} else {
				super.buttonPressed(buttonId);
				imageCanvas.dispose();
			}
		}

		@Override
		protected void okPressed() {
			applySettings(getLayerSupport().getViewer(), ImageLayer.this);
			super.okPressed();
		}

		@Override
		protected void cancelPressed() {
			redrawLayer(getLayerSupport().getViewer());
			super.cancelPressed();
		}

		private void redrawLayer(GridViewer viewer) {
			// Clear cache
			if (renderer != null) renderer.resetRendering();
			// Do actual redraw
			viewer.getGrid().redraw();
		}

		private void loadSample(Well well) {
			sampleWell = well;
			controlPanel.setImage(sampleWell);
			// Loading the image will also change newScale, keep current value.
			float originalScale = newScale;
			imageCanvas.loadImage(sampleWell);
			// Set newScale back to the actual value.
			newScale = originalScale;
			if (newScale == 0f) calculateFitScale();
			float tempScale = newScale;
			Rectangle tempRect = newRegion != null ? new Rectangle(newRegion.x, newRegion.y, newRegion.width, newRegion.height) : null;

			if (tempRect != null) {
				imageCanvas.changeScaleAndImageOffset(tempScale, tempRect.x, tempRect.y);
			} else {
				imageCanvas.changeScale(tempScale);
			}
		}

		private void calculateFitScale() {
			Point size = imageCanvas.getCurrentImageSize();
			// Fix: stick to pow2 scales, other values will cause glitches.
			newScale = 1f;
			while ((newScale * size.x) > lastW || (newScale * size.y) > lastH) newScale /= 2;
		}
	}
}
