package eu.openanalytics.phaedra.ui.columbus.util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;


public class ImageChannelConfigItem {

	private static final int THUMB_SIZE = 50;
	
	private ImageChannel channel;
	private ImageData thumb;
	
	private Composite control;
	private Button moveUpBtn;
	private Button moveDownBtn;
	private Text nameTxt;
	private ColorSelector colorSelector;
	private ContrastSlider contrastSlider;
	private Label thumbLbl;
	private Button deleteBtn;
	
	private AdvancedToolTip tooltip;
	
	public ImageChannelConfigItem(Composite parent, ImageChannel channel, ImageData thumb, ImageChannelOrderManager orderMgr) {
		this.channel = channel;
		this.thumb = thumb;
		
		control = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(control);
		
		Composite comp = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(comp);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(comp);
		
		moveUpBtn = new Button(comp, SWT.PUSH);
		moveUpBtn.setImage(IconManager.getIconImage("arrow_up.png"));
		moveUpBtn.setToolTipText("Move channel up");
		
		moveDownBtn = new Button(comp, SWT.PUSH);
		moveDownBtn.setImage(IconManager.getIconImage("arrow_down.png"));
		moveDownBtn.setToolTipText("Move channel down");
		
		nameTxt = new Text(control, SWT.BORDER);
		nameTxt.setText(channel.getName());
		nameTxt.addModifyListener(e -> channel.setName(nameTxt.getText()));
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(nameTxt);
		
		colorSelector = new ColorSelector(control);
		colorSelector.setColorValue(ColorUtils.hexToRgb(channel.getColorMask()));
		colorSelector.addListener(e -> {
			channel.setColorMask(ColorUtils.rgbToHex(colorSelector.getColorValue()));
			refreshThumb();
		});
		
		if (channel.getType() == ImageChannel.CHANNEL_TYPE_RAW) {
			contrastSlider = new ContrastSlider(control, channel.getLevelMin(), channel.getLevelMax(), channel.getBitDepth(), (l, h) -> {
				channel.setLevelMin(l);
				channel.setLevelMax(h);
				refreshThumb();
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(contrastSlider.getControl());
		} else {
			Label lbl = new Label(control, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);
		}
		
		thumbLbl = new Label(control, SWT.NONE);
		GridDataFactory.fillDefaults().hint(THUMB_SIZE, THUMB_SIZE).applyTo(thumbLbl);
		
		deleteBtn = new Button(control, SWT.PUSH);
		deleteBtn.setImage(IconManager.getIconImage("bin.png"));
		deleteBtn.setToolTipText("Remove this channel");

		tooltip = new AdvancedToolTip(thumbLbl, AdvancedToolTip.NO_RECREATE, false) {
			@Override
			public Object getData(Event event) {
				return thumbLbl;
			}
		};
		tooltip.setLabelProvider(new ToolTipLabelProvider() {
			public Image getImage(Object element) {
				Image largeImage = (Image) thumbLbl.getData("largeImage");
				return largeImage == null ? thumbLbl.getImage() : largeImage;
			};
			public String getText(Object element) {
				return channel.getName();
			};
		});
		tooltip.activate();
		
		if (orderMgr != null) orderMgr.registerChannel(channel, control, moveUpBtn, moveDownBtn, deleteBtn);
		refreshThumb();
	}
	
	public Composite getControl() {
		return control;
	}
	
	private void refreshThumb() {
		if (thumb == null) return;
		if (thumbLbl.getImage() != null) {
			 if (!thumbLbl.getImage().isDisposed()) thumbLbl.getImage().dispose();
			 Image largeImage = (Image) thumbLbl.getData("largeImage");
			 if (largeImage != null && !largeImage.isDisposed()) largeImage.dispose();
		}
		
		IComponentType type = ComponentTypeFactory.getInstance().getComponent(channel);
		channel.getChannelConfig().put("colorMask", ColorUtils.createRGBString(ColorUtils.hexToRgb(channel.getColorMask())));
		type.loadConfig(channel.getChannelConfig());
		
		ImageData output = new ImageData(thumb.width, thumb.height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		type.blend(thumb, output, new int[] { 0, 0, 0, channel.getLevelMin(), channel.getLevelMax(), channel.getAlpha() });
		
		Image largeImage = new Image(null, output);
		Image image = ImageUtils.scaleByAspectRatio(largeImage, THUMB_SIZE, THUMB_SIZE, false);
		thumbLbl.setImage(image);
		thumbLbl.setData("largeImage", largeImage);
	}
}
