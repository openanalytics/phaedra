package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;

public class ImagePreviewDialog extends TitleAreaDialog {

	private ImageChannel channel;
	private ImageData imageData;
	
	private Label imageLbl;
	private ContrastSlider contrastSlider;
	
	public ImagePreviewDialog(Shell parentShell, ImageChannel channel, ImageData imageData) {
		super(parentShell);
		this.channel = channel;
		this.imageData = imageData;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Image Preview");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		imageLbl = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).align(SWT.CENTER, SWT.CENTER).applyTo(imageLbl);
		
		contrastSlider = new ContrastSlider(parent, channel.getLevelMin(), channel.getLevelMax(), channel.getBitDepth(), (l, h) -> {
			channel.setLevelMin(l);
			channel.setLevelMax(h);
			refreshThumb();
		});
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(0, 5).applyTo(contrastSlider.getControl());
		
		setTitle("Image Preview");
		setMessage("A sample image is shown below. You can adjust the contrast if needed.");

		refreshThumb();		
		main.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				refreshThumb();
			}
		});
		
		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
	}
	
	private void refreshThumb() {
		IComponentType type = ComponentTypeFactory.getInstance().getComponent(channel);
		channel.getChannelConfig().put("colorMask", ColorUtils.createRGBString(ColorUtils.hexToRgb(channel.getColorMask())));
		type.loadConfig(channel.getChannelConfig());
		
		ImageData output = new ImageData(imageData.width, imageData.height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		type.blend(imageData, output, new int[] { 0, 0, 0, channel.getLevelMin(), channel.getLevelMax(), channel.getAlpha() });
		
		Point size = imageLbl.getParent().getSize();
		int dim = Math.min(size.x, size.y);
		if (dim == 0) dim = 300;
		Image image = ImageUtils.scaleByAspectRatio(new Image(null, output), dim, dim, true);
		
		if (imageLbl.getImage() != null && !imageLbl.getImage().isDisposed()) imageLbl.getImage().dispose();
		imageLbl.setImage(image);
		imageLbl.getParent().layout();
	}
}
