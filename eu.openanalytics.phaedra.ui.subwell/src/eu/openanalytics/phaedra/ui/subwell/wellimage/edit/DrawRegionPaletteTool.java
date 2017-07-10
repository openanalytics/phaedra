package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.CompressionConfig;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.ui.subwell.SubWellClassificationSupport;
import eu.openanalytics.phaedra.ui.wellimage.util.LabelImageFactory;
import eu.openanalytics.phaedra.validation.ValidationUtils;

public class DrawRegionPaletteTool extends AbstractPaletteTool {

	private Composite regionBtnContainer;

	private Combo featureCmb;
	private Combo channelCmb;

	private Label regionSizeLbl;
	private Label itemCountLbl;

	private SubWellFeature[] classificationFeatures;
	private SubWellFeature classificationFeature;
	private FeatureClass currentClass;

	private String[] channels;
	private int currentChannel;

	@Override
	public String getLabel() {
		return "Regions";
	}

	@Override
	public void createUI(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Draw new region:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

		regionBtnContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2,1).applyTo(regionBtnContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(regionBtnContainer);

		lbl = new Label(regionBtnContainer, SWT.NONE);
		lbl.setText("<No classes available>");

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Feature:");

		featureCmb = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		featureCmb.addListener(SWT.Selection, e -> changeClassificationFeature());
		List<String> featureNames = Arrays.stream(classificationFeatures).map(f -> f.getName()).collect(Collectors.toList());
		featureCmb.setItems(featureNames.toArray(new String[featureNames.size()]));
		if (classificationFeature != null) featureCmb.select(CollectionUtils.find(classificationFeatures, classificationFeature));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(featureCmb);
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText("In channel:");

		channelCmb = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		channelCmb.addListener(SWT.Selection, e -> currentChannel = channelCmb.getSelectionIndex());
		channelCmb.setItems(channels);
		channelCmb.select(currentChannel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channelCmb);

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Region size:");

		regionSizeLbl = new Label(container, SWT.NONE);
		regionSizeLbl.setText("0 px");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(regionSizeLbl);

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Items in region:");

		itemCountLbl = new Label(container, SWT.NONE);
		itemCountLbl.setText("0");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(itemCountLbl);

		createButtons(container);
		createDrawButtons();

		regionBtnContainer.addListener(SWT.Dispose, e -> {
			for (Control c: regionBtnContainer.getChildren()) {
				if (c instanceof Button) {
					Button b = (Button)c;
					if (b.getImage() != null) b.getImage().dispose();
				}
				c.dispose();
			}
		});
	}

	@Override
	protected void loadInitial(IValueObject inputObject) {
		ProtocolClass pClass = SelectionUtils.getAsClass(inputObject, ProtocolClass.class);
		
		classificationFeatures = ClassificationService.getInstance().findSubWellClassificationFeatures(pClass).toArray(new SubWellFeature[0]);
		if (classificationFeatures.length > 0) classificationFeature = classificationFeatures[0];

		List<ImageChannel> channelList = pClass.getImageSettings().getImageChannels();
		channels = new String[channelList.size()];
		int selectionIndex = (channels.length > 0) ? 0 : -1;
		for (int i=0; i<channelList.size(); i++) {
			channels[i] = channelList.get(i).getName();
			if (channels[i].toLowerCase().contains("region")) selectionIndex = i;
		}
		currentChannel = selectionIndex;
	}

	@Override
	protected AbstractDrawnObject generateObject(PathData path, ISelection selection) {
		if (currentClass == null) throw new IllegalStateException("Cannot add region: no feature class selected");
		toggleDrawButton(null);
		getHost().toggleDrawMode(false);

		Region newRegion = new Region();
		newRegion.path = path;
		newRegion.featureClass = currentClass;
		newRegion.color = ColorUtils.hexToRgb(currentClass.getRgbColor());

		// Keep track of the selected subwell items.
		SubWellSelection swSelection = SelectionUtils.getFirstObject(selection, SubWellSelection.class);
		newRegion.items = new SubWellItem[swSelection.getIndices().cardinality()];
		BitSet indices = swSelection.getIndices();
		int index = 0;
		for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i+1)) {
			newRegion.items[index++] = new SubWellItem(swSelection.getWell(), i);
		}
		newRegion.size = SWTUtils.getSurface(path);

		String sizeString = Formatters.getInstance().format((int)newRegion.size, "###,###,###") + " px";
		regionSizeLbl.setText(sizeString);
		itemCountLbl.setText("" + newRegion.items.length);

		return newRegion;
	}

	@Override
	protected void doSave(IProgressMonitor monitor) throws IOException {
		Map<Well, List<AbstractDrawnObject>> regionsPerWell = getDrawnObjects();
		
		List<Plate> plates = regionsPerWell.keySet().stream().map(w -> w.getPlate()).distinct().collect(Collectors.toList());
		if (plates.size() > 1) throw new UnsupportedOperationException("Cannot save region classification for more than 1 plate at a time");
		if (plates.isEmpty()) return;
		Plate plate = plates.get(0);
		ValidationUtils.checkCanModifyPlate(plate);

		monitor.beginTask("Saving regions, this may take several minutes...", 15+10*regionsPerWell.size());

		String tempPath = FileUtils.generateTempFolder(true);
		SubWellClassificationSupport classification = new SubWellClassificationSupport();
		Map<Integer, String> labelImages = new HashMap<>();

		monitor.subTask("Creating label images");
		for (Well well: regionsPerWell.keySet()) {
			List<AbstractDrawnObject> regions = regionsPerWell.get(well);

			List<PathData> paths = new ArrayList<>();
			List<FeatureClass> classes = new ArrayList<>();

			for (AbstractDrawnObject object: regions) {
				Region region = (Region)object;
				// Create a classification batch for each region.
				classification.getCurrentBatch().setItems(region.items);
				classification.getCurrentBatch().setFeatureClass(region.featureClass);
				classification.addBatch();

				paths.add(region.path);
				classes.add(region.featureClass);
			}

			// Create a label image for this well.
			ImageData labelImage = LabelImageFactory.createLabelImage(paths, classes, well, currentChannel, new SubProgressMonitor(monitor, 8));

			int wellNr = PlateUtils.getWellNr(well);
			String labelImagePath = tempPath + "/" + wellNr + ".j2c";

			try (IEncodeAPI compressor = CodecFactory.getEncoder()) {
				CompressionConfig config = new CompressionConfig();
				config.reversible = true;
				compressor.compressCodeStream(config, labelImage, labelImagePath);
				monitor.worked(2);
			}

			labelImages.put(wellNr, labelImagePath);
		}

		// Update classification data.
		classification.save(new SubProgressMonitor(monitor, 5));

		// Collect region size in well feature(s).
		Map<Feature, double[]> valuesToSave = new HashMap<>();
		for (Well well: regionsPerWell.keySet()) {
			PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
			List<AbstractDrawnObject> regions = regionsPerWell.get(well);

			for (AbstractDrawnObject object: regions) {
				Region region = (Region)object;
				String regionName = region.featureClass.getLabel().toLowerCase();
				String featureName = String.format("%sSizePixels", regionName);

				Feature feature = ProtocolUtils.getFeatureByName(featureName, PlateUtils.getProtocolClass(well));
				if (feature == null) continue;
				
				double[] featureValues = valuesToSave.get(feature);
				if (featureValues == null) {
					// First access: look up existing values (if any).
					featureValues = new double[PlateUtils.getWellCount(plate)];
					for (int i = 0; i < featureValues.length; i++) {
						featureValues[i] = accessor.getNumericValue(i+1, feature, null);
					}
					valuesToSave.put(feature, featureValues);
				}

				int wellNr = PlateUtils.getWellNr(well);
				double oldValue = featureValues[wellNr-1];
				featureValues[wellNr-1] = (Double.isNaN(oldValue)) ? region.size : (oldValue + region.size);
				ObjectLogService.getInstance().logFeatureChange(well, feature.getDisplayName(), ""+oldValue, ""+featureValues[wellNr-1], "");
			}
		}

		// Save the modified values and recalculate the plate.
		for (Feature f: valuesToSave.keySet()) {
			double[] values = valuesToSave.get(f);
			if (values != null) PlateService.getInstance().updateWellDataRaw(plate, f, values);
		}
		
		// Since raw values have changed, do a full reset of the data accessor, then recalculate.
		CalculationService.getInstance().getAccessor(plate).reset();
		CalculationService.getInstance().calculate(plate);

		// Translate the well nr to a codestream nr.
		ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
		Map<Integer, String> codestreams = new HashMap<>();
		for (int wellNr: labelImages.keySet()) {
			int codestreamNr = currentChannel + (wellNr-1)*pClass.getImageSettings().getImageChannels().size();
			codestreams.put(codestreamNr, labelImages.get(wellNr));
		}

		// Update the image file with the modified codestreams.
		monitor.subTask("Updating plate label images");
		String imagePath = PlateService.getInstance().getImageFSPath(plate);
		String newImagePath = tempPath + "/updated." + FileUtils.getExtension(imagePath).toLowerCase();
		
		try (IEncodeAPI compressor = CodecFactory.getEncoder()) {
			InputStream input = Screening.getEnvironment().getFileServer().getContents(imagePath);
			compressor.updateCodestreamFile(input, codestreams, newImagePath, new SubProgressMonitor(monitor, 5));
		}
			
		String relativeImagePath = PlateService.getInstance().getImageFSPath(plate);
		Screening.getEnvironment().getFileServer().safeReplace(relativeImagePath, new File(newImagePath));
		monitor.worked(5);

		monitor.done();
	}

	private void changeClassificationFeature() {
		classificationFeature = classificationFeatures[featureCmb.getSelectionIndex()];
		createDrawButtons();
	}
	
	private void createDrawButtons() {
		if (classificationFeature == null) return;
		List<FeatureClass> classes = classificationFeature.getFeatureClasses();

		// Clear any existing controls before adding new ones.
		for (Control c: regionBtnContainer.getChildren()) {
			if (c instanceof Button) {
				Button b = (Button)c;
				if (b.getImage() != null) b.getImage().dispose();
			}
			c.dispose();
		}

		for (FeatureClass fc: classes) {
			final FeatureClass classToDraw = fc;
			Button drawRegionBtn = new Button(regionBtnContainer, SWT.TOGGLE);
			drawRegionBtn.setText(fc.getLabel());
			drawRegionBtn.setImage(createIcon(classToDraw));
			drawRegionBtn.addListener(SWT.Selection, e -> {
				Button btn = (Button)e.widget;
				if (btn.getSelection()) {
					currentClass = classToDraw;
					toggleDrawButton(btn);
					start();
				} else {
					currentClass = null;
					cancel();
				}
			});
		}

		GridDataFactory.fillDefaults().grab(true, false).span(2,1).applyTo(regionBtnContainer);
		regionBtnContainer.layout();
		regionBtnContainer.getParent().layout();
	}

	private Image createIcon(FeatureClass fc) {
		int w = 20;
		int h = 20;

		RGB rgbColor = ColorUtils.hexToRgb(fc.getRgbColor());
		Color color =  new Color(PlatformUI.getWorkbench().getDisplay(), rgbColor);

		Image img = new Image(null, w, h);
		GC gc = new GC(img);

		gc.setAntialias(SWT.ON);
		PlotShape ps = PlotShape.valueOf(fc.getSymbol());
		gc.setForeground(color);
		gc.setBackground(color);
		ps.drawShape(gc, w/2, h/2, 5, true);

		gc.dispose();
		color.dispose();
		return img;
	}

	private void toggleDrawButton(Button btn) {
		if (btn != null) btn.setSelection(true);
		for (Control c: regionBtnContainer.getChildren()) {
			if (c instanceof Button) {
				if (c != btn) ((Button)c).setSelection(false);
			}
		}
	}

	private static class Region extends AbstractDrawnObject {
		public float size;
		public SubWellItem[] items;
		public FeatureClass featureClass;
	}
}
