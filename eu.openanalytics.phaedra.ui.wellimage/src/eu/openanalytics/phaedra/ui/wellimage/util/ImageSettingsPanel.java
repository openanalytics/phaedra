package eu.openanalytics.phaedra.ui.wellimage.util;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;

public class ImageSettingsPanel extends Composite {

	private Scale gamma;
	private Text gammaTxt;

	private Combo channelCmb;
	private Scale channelAlpha;
	private Scale channelMin;
	private Scale channelMax;

	private Label channelTypeLbl;
	private Text alphaTxt;
	private Text minTxt;
	private Text maxTxt;

	private Label histogramInfoLbl;
	private Canvas histogramCanvas;
	private Button histogramLogBtn;
	private Button drawGammaCurveBtn;

	private Button resetBtn;

	private ImageSettings currentSettings;
	private ImageChannel currentChannel;
	private Well currentWell;
	private Image currentHistogram;

	private boolean drawGammaCurve = true;

	private SplitComposite splitComp;

	public ImageSettingsPanel(Composite parent, int style) {
		super(parent, style);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this);
		GridLayoutFactory.fillDefaults().applyTo(this);

		// Create Split Composite
		SplitCompositeFactory.getInstance().prepare(null, SplitComposite.MODE_V_1_2);
		splitComp = SplitCompositeFactory.getInstance().create(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComp);
		GridLayoutFactory.fillDefaults().applyTo(splitComp);

		ScrolledComposite settingsScroll = new ScrolledComposite(splitComp, SWT.H_SCROLL | SWT.V_SCROLL);
		settingsScroll.setExpandHorizontal(true);
		settingsScroll.setExpandVertical(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(settingsScroll);

		Composite settingsContainer = new Composite(settingsScroll, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(3, 3).applyTo(settingsContainer);

		settingsScroll.setContent(settingsContainer);

		ScrolledComposite histogramScroll = new ScrolledComposite(splitComp, SWT.H_SCROLL | SWT.V_SCROLL);
		histogramScroll.setExpandHorizontal(true);
		histogramScroll.setExpandVertical(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(histogramScroll);

		Composite histogramContainer = new Composite(histogramScroll, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(3, 3).applyTo(histogramContainer);

		histogramScroll.setContent(histogramContainer);

		/*
		 * Group: image settings
		 */

		Group imgGroup = new Group(settingsContainer, SWT.SHADOW_ETCHED_IN);
		imgGroup.setText("Image Settings");
		GridLayoutFactory.fillDefaults().numColumns(3).margins(2, 2).spacing(3, 2).applyTo(imgGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(imgGroup);

		Label lbl = new Label(imgGroup, SWT.NONE);
		lbl.setText("Gamma:");

		gammaTxt = new Text(imgGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(20, SWT.DEFAULT).applyTo(gammaTxt);

		gamma = new Scale(imgGroup, SWT.HORIZONTAL);
		gamma.setMaximum(30);
		gamma.addListener(SWT.Selection, e -> updateGamma());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gamma);

		/*
		 * Group: channel settings
		 */

		Group channelGroup = new Group(settingsContainer, SWT.SHADOW_ETCHED_IN);
		channelGroup.setText("Channel Settings");
		GridLayoutFactory.fillDefaults().numColumns(4).margins(2, 2).spacing(3, 2).applyTo(channelGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelGroup);

		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("color_wheel.png"));
		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setText("Channel:");

		channelTypeLbl = new Label(channelGroup, SWT.NONE);
		GridDataFactory.fillDefaults().hint(20,20).align(SWT.CENTER, SWT.CENTER).applyTo(channelTypeLbl);

		channelCmb = new Combo(channelGroup, SWT.READ_ONLY);
		channelCmb.addListener(SWT.Selection, e -> {
			if (currentSettings == null) return;
			int i = channelCmb.getSelectionIndex();
			ImageChannel channel = currentSettings.getImageChannels().get(i);
			loadChannel(channel);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelCmb);

		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("alpha.png"));
		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setText("Alpha:");

		alphaTxt = new Text(channelGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(alphaTxt);

		channelAlpha = new Scale(channelGroup, SWT.HORIZONTAL);
		channelAlpha.setMaximum(255);
		channelAlpha.addListener(SWT.Selection, e -> updateAlpha());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelAlpha);

		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("contrast_low.png"));
		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setText("Min:");

		minTxt = new Text(channelGroup, SWT.BORDER);
		minTxt.addListener(SWT.Modify, e -> {
			if (e.widget != channelMin) {
				String txt = minTxt.getText();
				if (NumberUtils.isNumeric(txt)) {
					int value = Integer.parseInt(txt);
					if (value != channelMin.getSelection()) {
						channelMin.setSelection(value);
						updateMin();
					}
				}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(minTxt);

		channelMin = new Scale(channelGroup, SWT.HORIZONTAL);
		channelMin.setMaximum(255);
		channelMin.addListener(SWT.Selection, e -> updateMin());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelMin);

		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("contrast_high.png"));
		lbl = new Label(channelGroup, SWT.NONE);
		lbl.setText("Max:");

		maxTxt = new Text(channelGroup, SWT.BORDER);
		maxTxt.addListener(SWT.Modify, e -> {
			if (e.widget != channelMax) {
				String txt = maxTxt.getText();
				if (NumberUtils.isNumeric(txt)) {
					int value = Integer.parseInt(txt);
					if (value != channelMax.getSelection()) {
						channelMax.setSelection(value);
						updateMax();
					}
				}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(maxTxt);

		channelMax = new Scale(channelGroup, SWT.HORIZONTAL);
		channelMax.setMaximum(255);
		channelMax.addListener(SWT.Selection, e -> updateMax());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelMax);

		resetBtn = new Button(settingsContainer, SWT.PUSH);
		resetBtn.setText("Reset defaults");
		resetBtn.addListener(SWT.Selection, e -> ImageSettingsService.getInstance().resetSettings());

		/*
		 * Group: color histogram
		 */

		Group histogramGroup = new Group(histogramContainer, SWT.SHADOW_ETCHED_IN);
		histogramGroup.setText("Color Histogram");
		GridLayoutFactory.fillDefaults().numColumns(2).margins(1, 1).spacing(1, 1).applyTo(histogramGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(histogramGroup);

		histogramInfoLbl = new Label(histogramGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(histogramInfoLbl);

		histogramCanvas = new Canvas(histogramGroup, SWT.BORDER);
		histogramCanvas.addListener(SWT.Paint, e -> drawHistogram(e.gc));
		histogramCanvas.addListener(SWT.Resize, e -> loadHistogram());
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(histogramCanvas);

		histogramLogBtn = new Button(histogramGroup, SWT.CHECK);
		histogramLogBtn.setText("Log.");
		histogramLogBtn.addListener(SWT.Selection, e -> loadHistogram());

		drawGammaCurveBtn = new Button(histogramGroup, SWT.CHECK);
		drawGammaCurveBtn.setText("Gamma Curve");
		drawGammaCurveBtn.setSelection(drawGammaCurve);
		drawGammaCurveBtn.addListener(SWT.Selection, e -> {
			drawGammaCurve = !drawGammaCurve;
			histogramCanvas.redraw();
		});

		settingsScroll.setMinSize(settingsContainer.computeSize(200, SWT.DEFAULT));
		settingsContainer.layout();

		histogramScroll.setMinSize(histogramContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		histogramContainer.layout();

		splitComp.setWeights(new int[] { 50, 50 });
	}

	@Override
	public boolean setFocus() {
		if (gamma.setFocus()) return true;
		return super.setFocus();
	}

	public void load(Well well) {
		if (currentWell != null && currentWell.equals(well)) return;
		currentWell = well;
		loadHistogram();
	}

	public void load(ImageSettings settings) {
		loadSettings(settings);
	}

	public ContributionItem getModeButton() {
		return splitComp.createModeButton();
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void loadSettings(ImageSettings settings) {
		if (settings == null) return;
		if (currentSettings == settings) return;

		currentSettings = settings;

		gamma.setSelection(settings.getGamma());
		gammaTxt.setText(gammaToDisplay(settings.getGamma()));

		List<ImageChannel> channels = settings.getImageChannels();
		String[] channelNames = new String[channels.size()];
		for (int i=0; i<channels.size(); i++) channelNames[i] = channels.get(i).getName();
		channelCmb.setItems(channelNames);

		if (!channels.isEmpty()) {
			loadChannel(settings.getImageChannels().get(0));
		}
	}


	private void loadChannel(ImageChannel channel) {
		if (currentChannel == channel) return;
		currentChannel = channel;

		int index = channel.getImageSettings().getImageChannels().indexOf(channel);
		channelCmb.select(index);

		IComponentType type = ComponentTypeFactory.getInstance().getComponent(channel);
		Image icon = type.createIcon(Display.getCurrent());
		if (channelTypeLbl.getImage() != null) channelTypeLbl.getImage().dispose();
		channelTypeLbl.setImage(icon);

		channelAlpha.setSelection(channel.getAlpha());
		int maxValue = ImageUtils.getMaxColors(channel.getBitDepth());
		channelMin.setMaximum(maxValue);
		channelMax.setMaximum(maxValue);
		channelMin.setSelection(channel.getLevelMin());
		channelMax.setSelection(channel.getLevelMax());

		alphaTxt.setText("" + channel.getAlpha());
		minTxt.setText("" + channel.getLevelMin());
		maxTxt.setText("" + channel.getLevelMax());

		boolean isRaw = (type.getId() == ImageChannel.CHANNEL_TYPE_RAW);
		channelMin.setEnabled(isRaw);
		channelMax.setEnabled(isRaw);
		minTxt.setEnabled(isRaw);
		maxTxt.setEnabled(isRaw);
		channelAlpha.setEnabled(!isRaw);
		alphaTxt.setEnabled(!isRaw);

		loadHistogram();
	}

	private void loadHistogram() {
		IDecodeAPI imageFile = null;
		try {
			if (currentWell != null && currentWell.getPlate().isImageAvailable() && currentChannel != null) {

				histogramInfoLbl.setText(currentChannel.getName()
						+ " @ Well " + NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn()));

				// Open the new JP2K file.
				Plate plate = currentWell.getPlate();
				String filePath = PlateService.getInstance().getImagePath(plate);
				imageFile = CodecFactory.getDecoder(filePath, PlateUtils.getWellCount(plate), currentSettings.getImageChannels().size());
				imageFile.open();

				// -4 is to make up for the Canvas border.
				int w = histogramCanvas.getSize().x - 4;
				int h = histogramCanvas.getSize().y - 4;

				int nr = NumberUtils.getWellNr(currentWell.getRow(), currentWell.getColumn(), currentWell.getPlate().getColumns());
				ImageData wellImage = imageFile.renderImage(400, 400, nr-1, currentChannel.getSequence());
				Image histogram = ColorHistogramFactory.createHistogram(w, h, wellImage, currentChannel,
						((float)currentSettings.getGamma())/10, histogramLogBtn.getSelection());
				if (currentHistogram != null) currentHistogram.dispose();
				currentHistogram = histogram;
				histogramCanvas.redraw();
			} else {
				// Blank out the histogram.
				histogramInfoLbl.setText("No image data available");
				if (currentHistogram != null) currentHistogram.dispose();
				currentHistogram = null;
				histogramCanvas.redraw();
			}
		} catch (IOException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Image Error", "The JP2K file could not be opened: " + e.getMessage());
		} finally {
			if (imageFile != null) imageFile.close();
		}
	}

	private void drawHistogram(GC gc) {
		if (currentHistogram == null) return;
		gc.drawImage(currentHistogram, 0, 0);

		if (currentChannel != null) {
			Color green = gc.getDevice().getSystemColor(SWT.COLOR_GREEN);
			Color red = gc.getDevice().getSystemColor(SWT.COLOR_RED);
			Color gray = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);

			Point size = histogramCanvas.getSize();

			int min = currentChannel.getLevelMin();
			int max = currentChannel.getLevelMax();

			int rangeMax = ImageUtils.getMaxColors(currentChannel.getBitDepth());
			min = (int)(((double)min/rangeMax)*(size.x-5));
			max = (int)(((double)max/rangeMax)*(size.x-5));

			gc.setLineWidth(3);
			gc.setForeground(green);
			gc.drawLine(min, 0, min, size.y);

			if (min > 0) {
				gc.setAlpha(100);
				gc.setBackground(gray);
				gc.fillRectangle(0, 0, min, size.y);
				gc.setAlpha(255);
			}

			gc.setForeground(red);
			gc.drawLine(max, 0, max, histogramCanvas.getSize().y);

			if (max > 0) {
				gc.setAlpha(100);
				gc.setBackground(gray);
				gc.fillRectangle(max, 0, size.x-max, size.y);
				gc.setAlpha(255);
			}

			if (drawGammaCurve) drawGammaCurve(gc);
		}
	}

	private void drawGammaCurve(GC gc) {
		Point size = histogramCanvas.getSize();
		int min = currentChannel.getLevelMin();
		int max = currentChannel.getLevelMax();

		int rangeMax = ImageUtils.getMaxColors(currentChannel.getBitDepth());
		min = (int)(((double)min/rangeMax)*(size.x-5));
		max = (int)(((double)max/rangeMax)*(size.x-5));

		Color blue = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);

		gc.setLineWidth(2);
		gc.setForeground(blue);
		gc.setLineStyle(SWT.LINE_SOLID);
		if (max == 255)	max--;
		gc.drawLine(0, size.y-4, min, size.y-4);

		float xRange = max - min;
		float yRange = size.y-4;
		float xOffset = min;

		int steps = (int)(xRange/3);
		if (steps < 3) steps = 3;
		int [] gamPoints = new int[steps * 2];
		int preGammaVal;
		for (int i=0; i<steps; i++){
			gamPoints[(i*2)] = (int)(xOffset + i * (xRange/steps));
			preGammaVal = (int)(i * (yRange/steps));
			preGammaVal = (int)(Math.pow(preGammaVal/yRange,1f/(currentSettings.getGamma()/10f))*yRange);
			gamPoints[(i*2)+1] = (int) yRange - preGammaVal;
		}

		//correct the last points to make them fit
		gamPoints[(steps-1) * 2] = max;
		gamPoints[(steps-1) * 2 + 1] = 0;

		int x1, y1, x2, y2;
		for (int i=0; i<steps-1; i++){
			x1 = gamPoints[(i*2)];
			y1 = gamPoints[(i*2)+1];
			x2 = gamPoints[(i*2)+2];
			y2 = gamPoints[(i*2)+3];
			gc.drawLine(x1, y1, x2, y2);
		}

		gc.drawLine(max, 0, size.x-4, 0);
	}

	private void updateGamma() {
		int v = gamma.getSelection();
		gammaTxt.setText(gammaToDisplay(v));
		currentSettings.setGamma(v);
		histogramCanvas.redraw();
		delayedNotifySettings();
	}

	private void updateAlpha() {
		int v = channelAlpha.getSelection();
		alphaTxt.setText("" + v);
		currentChannel.setAlpha(v);
		delayedNotifySettings();
	}

	private void updateMin() {
		int v = channelMin.getSelection();
		int maxV = channelMax.getSelection();
		if (v > maxV) {
			v = maxV;
			channelMin.setSelection(v);
		}
		String txtVal = "" + v;
		minTxt.setText(txtVal);
		minTxt.setSelection(txtVal.length());
		currentChannel.setLevelMin(v);
		histogramCanvas.redraw();
		delayedNotifySettings();
	}

	private void updateMax() {
		int v = channelMax.getSelection();
		int minV = channelMin.getSelection();
		if (v < minV) {
			v = minV;
			channelMax.setSelection(v);
		}
		String txtVal = "" + v;
		maxTxt.setText(txtVal);
		maxTxt.setSelection(txtVal.length());
		currentChannel.setLevelMax(v);
		histogramCanvas.redraw();
		delayedNotifySettings();
	}

	private void delayedNotifySettings() {
		JobUtils.runJob(monitor -> {
			Display.getDefault().asyncExec(() -> ImageSettingsService.getInstance().notifySettingsChanged());
		}, false, "Notify Settings Changed", 100, toString(), null, 500);
	}

	private String gammaToDisplay(int gamma) {
		double v = ((double)gamma)/10;
		return NumberUtils.round(v, 1);
	}
}
