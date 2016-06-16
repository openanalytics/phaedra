package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

class ChannelRow extends Composite {

	private ImageChannel channel;
	
	private Label sequenceLbl;
	private Text nameTxt;
	private ColorSelector colorMaskBtn;
	private Label patternLbl;
	
	public ChannelRow(ChannelComposer composer, ImageChannel channel) {
		super(composer.getChannelRowArea(), SWT.BORDER);
		this.channel = channel;
		GridLayoutFactory.fillDefaults().numColumns(9).spacing(0, 0).applyTo(this);
		
		sequenceLbl = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().hint(15, SWT.DEFAULT).indent(5, 0).align(SWT.BEGINNING, SWT.CENTER).applyTo(sequenceLbl);
		
		Button moveUpBtn = new Button(this, SWT.PUSH);
		moveUpBtn.setImage(IconManager.getIconImage("arrow_up.png"));
		moveUpBtn.setToolTipText("Move channel up");
		moveUpBtn.addListener(SWT.Selection, e -> composer.moveChannelUp(channel));
		
		Button moveDownBtn = new Button(this, SWT.PUSH);
		moveDownBtn.setImage(IconManager.getIconImage("arrow_down.png"));
		moveDownBtn.setToolTipText("Move channel down");
		moveDownBtn.addListener(SWT.Selection, e -> composer.moveChannelDown(channel));
		
		nameTxt = new Text(this, SWT.BORDER);
		nameTxt.addModifyListener(e -> channel.setName(nameTxt.getText()));
		GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(nameTxt);
		
		colorMaskBtn = new ColorSelector(this);
		colorMaskBtn.addListener(e -> channel.setColorMask(ColorUtils.rgbToHex(colorMaskBtn.getColorValue())));
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(colorMaskBtn.getButton());
		
		patternLbl = new Label(this, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).indent(5, 0).applyTo(patternLbl);
		
		Button editPatternBtn = new Button(this, SWT.PUSH);
		editPatternBtn.setText("...");
		editPatternBtn.setToolTipText("Edit pattern");
		editPatternBtn.addListener(SWT.Selection, e -> editPattern());
		
		Button previewBtn = new Button(this, SWT.PUSH);
		previewBtn.setImage(IconManager.getIconImage("image.png"));
		previewBtn.setToolTipText("Preview");
		previewBtn.addListener(SWT.Selection, e -> previewImage());
		
		Button removeBtn = new Button(this, SWT.PUSH);
		removeBtn.setImage(IconManager.getIconImage("delete.png"));
		removeBtn.setToolTipText("Remove channel");
		removeBtn.addListener(SWT.Selection, e -> composer.removeChannel(channel));
		
		refresh();
	}

	public ImageChannel getChannel() {
		return channel;
	}
	
	public void refresh() {
		sequenceLbl.setText("" + channel.getSequence());
		nameTxt.setText(channel.getName());
		colorMaskBtn.setColorValue(ColorUtils.hexToRgb(channel.getColorMask()));
		patternLbl.setText(channel.getDescription());
	}
	
	private void editPattern() {
		//TODO
	}
	
	private void previewImage() {
		//TODO
//		String filePath = "C:/Dev/Testdata/cellprofiler/12175829_Phenix/outlines/003003-1-001001001.png";
		String filePath = "C:/Dev/Testdata/cellprofiler/ExampleFlyImages/01_POS002_D.TIF";
//		String filePath = "C:/Dev/Testdata/a549/A549-Exp5-plate1-RSV/001-001/results/Well0003_mode1_z000_t000_mosaic.tif";
		
		try {
			ImageData imageData = FileUtils.getExtension(filePath).equalsIgnoreCase("tif") ? TIFFCodec.read(filePath)[0] : new ImageLoader().load(filePath)[0];
			channel.setBitDepth(imageData.depth);
			channel.setLevelMin(0);
			channel.setLevelMax((int) Math.pow(2, imageData.depth) - 1);
			
			new ImagePreviewDialog(getShell(), channel, imageData).open();
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Cannot preview image", "Failed to create preview: " + e.getMessage());
		}
	}
}