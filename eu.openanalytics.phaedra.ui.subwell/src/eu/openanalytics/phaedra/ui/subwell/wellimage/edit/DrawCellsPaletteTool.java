package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.ui.wellimage.overlay.CellMeasurer;
import eu.openanalytics.phaedra.ui.wellimage.overlay.CellMeasurer.CellMeasurement;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequest;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequestFactory;

public class DrawCellsPaletteTool extends AbstractPaletteTool {

	private Button startBtn;
	private Button stopBtn;

	private ComboViewer xFeatureCmb;
	private ComboViewer yFeatureCmb;
	private ComboViewer sizeCmb;
	private ComboViewer classificationFeatureCmb;
	private RichTableViewer classTableViewer;
	private RichTableViewer cellsViewer;

	private Map<DrawnCell, Image> imageCache;
	private ColorStore colorStore;
	private CellMeasurer measurer;

	private SubWellFeature[] allFeatures;
	private SubWellFeature[] classificationFeatures;

	private SubWellFeature xFeature;
	private SubWellFeature yFeature;
	private SubWellFeature sizeFeature;
	private SubWellFeature classificationFeature;
	private FeatureClass currentClass;
	private FeatureMappings intensityFeatures;

	private RGB regionColor = new RGB(0,255,0);

	@Override
	public String getLabel() {
		return "Cells";
	}

	@Override
	public void restoreState(IMemento memento) {
		SubWellFeature memXFeature = PaletteStateHelper.getSubWellFeature(memento, "xFeature");
		if (memXFeature != null) xFeature = memXFeature;
		SubWellFeature memYFeature = PaletteStateHelper.getSubWellFeature(memento, "yFeature");
		if (memYFeature != null) yFeature = memYFeature;
		SubWellFeature memSizeFeature = PaletteStateHelper.getSubWellFeature(memento, "sizeFeature");
		if (memSizeFeature != null) sizeFeature = memSizeFeature;
		SubWellFeature memClassificationFeature = PaletteStateHelper.getSubWellFeature(memento, "classificationFeature");
		if (memClassificationFeature != null) classificationFeature = memClassificationFeature;
		if (intensityFeatures != null) intensityFeatures.load(memento);
	}

	@Override
	public void saveState(IMemento memento) {
		PaletteStateHelper.saveSubWellFeature(xFeature, memento, "xFeature");
		PaletteStateHelper.saveSubWellFeature(yFeature, memento, "yFeature");
		PaletteStateHelper.saveSubWellFeature(sizeFeature, memento, "sizeFeature");
		PaletteStateHelper.saveSubWellFeature(classificationFeature, memento, "classificationFeature");
		if (intensityFeatures != null) intensityFeatures.save(memento);
	}

	@Override
	public void createUI(Composite parent) {
		imageCache = new HashMap<>();
		colorStore = new ColorStore();

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(3, 3).numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		startBtn = new Button(container, SWT.TOGGLE);
		startBtn.setText("Start drawing");
		startBtn.setImage(IconManager.getIconImage("pencil.png"));
		startBtn.addListener(SWT.Selection, e -> start());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(startBtn);

		stopBtn = new Button(container, SWT.PUSH);
		stopBtn.setText("Stop drawing");
		stopBtn.setImage(IconManager.getIconImage("cancel.png"));
		stopBtn.addListener(SWT.Selection, e -> cancel());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(stopBtn);

		createButtons(container);

		Label lbl = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).indent(0, 5).applyTo(lbl);

		LabelProvider lblProvider = new LabelProvider() {
			@Override
			public String getText(Object element) {
				SubWellFeature f = (SubWellFeature)element;
				return f.getDisplayName();
			}
		};

		// A combo to select the X position feature.
		new Label(container, SWT.NONE).setText("X Position feature:");
		xFeatureCmb = new ComboViewer(container, SWT.READ_ONLY);
		xFeatureCmb.setContentProvider(new ArrayContentProvider());
		xFeatureCmb.setLabelProvider(lblProvider);
		xFeatureCmb.addSelectionChangedListener(e -> xFeature = SelectionUtils.getFirstObject(e.getSelection(), SubWellFeature.class));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(xFeatureCmb.getControl());

		// A combo to select the Y position feature.
		new Label(container, SWT.NONE).setText("Y Position feature:");
		yFeatureCmb = new ComboViewer(container, SWT.READ_ONLY);
		yFeatureCmb.setContentProvider(new ArrayContentProvider());
		yFeatureCmb.setLabelProvider(lblProvider);
		yFeatureCmb.addSelectionChangedListener(e -> yFeature = SelectionUtils.getFirstObject(e.getSelection(), SubWellFeature.class));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(yFeatureCmb.getControl());

		// A combo to select the area/length feature.
		new Label(container, SWT.NONE).setText("Area feature:");
		sizeCmb = new ComboViewer(container, SWT.READ_ONLY);
		sizeCmb.setContentProvider(new ArrayContentProvider());
		sizeCmb.setLabelProvider(lblProvider);
		sizeCmb.addSelectionChangedListener(e -> sizeFeature = SelectionUtils.getFirstObject(e.getSelection(), SubWellFeature.class));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sizeCmb.getControl());

		new Label(container, SWT.NONE).setText("Intensity features:");
		Button selectBtn = new Button(container, SWT.PUSH);
		selectBtn.setText("Select...");
		selectBtn.addListener(SWT.Selection, e -> {
			SelectIntensityFeaturesDialog dialog = new SelectIntensityFeaturesDialog(Display.getCurrent().getActiveShell(), intensityFeatures);
			dialog.open();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(selectBtn);

		// A combo to select the classification feature.
		new Label(container, SWT.NONE).setText("Class feature:");
		classificationFeatureCmb = new ComboViewer(container, SWT.READ_ONLY);
		classificationFeatureCmb.setContentProvider(new ArrayContentProvider());
		classificationFeatureCmb.setLabelProvider(lblProvider);
		classificationFeatureCmb.addSelectionChangedListener(e -> classificationFeature = SelectionUtils.getFirstObject(e.getSelection(), SubWellFeature.class));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(classificationFeatureCmb.getControl());

		// A table listing the available classification classes.
		classTableViewer = new RichTableViewer(container, SWT.BORDER);
		classTableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (classificationFeature == null) return new Object[0];
				return classificationFeature.getFeatureClasses().toArray();
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// Do nothing.
			}
			@Override
			public void dispose() {
				// Do nothing.
			}
		});
		classTableViewer.addSelectionChangedListener(e -> {
			FeatureClass clazz = SelectionUtils.getFirstObject(e.getSelection(), FeatureClass.class);
			if (clazz != null) currentClass = clazz;
		});
		classTableViewer.applyColumnConfig(configureClassColumns());
		GridDataFactory.fillDefaults().grab(true, false).span(2,1).hint(SWT.DEFAULT, 100).applyTo(classTableViewer.getControl());

		lbl = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).indent(0, 5).applyTo(lbl);

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Measured cells:");

		cellsViewer = new RichTableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		cellsViewer.getTable().addListener(SWT.MeasureItem, event -> event.height = 40);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(cellsViewer.getControl());

		List<Image> usedIcons = new ArrayList<>();
		TableViewerColumn col = new TableViewerColumn(cellsViewer, SWT.NONE);
		col.getColumn().setWidth(50);
		for (ImageChannel channel: intensityFeatures.channels) {
			IComponentType type = ComponentTypeFactory.getInstance().getComponent(channel);
			Image icon = type.createIcon(null);

			usedIcons.add(icon);

			col = new TableViewerColumn(cellsViewer, SWT.NONE);
			col.getColumn().setImage(icon);
			col.getColumn().setText("Int");
			col.getColumn().setWidth(70);
		}
		container.addListener(SWT.Dispose, e -> usedIcons.forEach(icon -> icon.dispose()));

		cellsViewer.setContentProvider(new ArrayContentProvider());
		cellsViewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				DrawnCell c = (DrawnCell)cell.getElement();
				if (cell.getColumnIndex() == 0) {
					try {
						Image img = imageCache.get(c);
						if (img == null) {
							Rectangle bbox = ImageUtils.getBoundingBox(c.path);
							ImageRenderRequest req = ImageRenderRequestFactory.forWell(getCurrentWell()).withRegion(bbox).build();
							ImageData data = ImageRenderService.getInstance().getImageData(req);
							img = new Image(null, data);
							imageCache.put(c, img);
						}
						cell.setImage(img);
					} catch (IOException e) {}
				} else {
					cell.setText("Avg: " + NumberUtils.round(c.meas.avgIntensities[cell.getColumnIndex()-1], 0)
							+ "\nMax: " + NumberUtils.round(c.meas.maxIntensities[cell.getColumnIndex()-1], 0));
				}
			}
		});

		loadInitialUI();
	}

	@Override
	public void dispose() {
		if (measurer != null) measurer.close();
		for (Image i: imageCache.values()) i.dispose();
		colorStore.dispose();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		cellsViewer.setInput(getCurrentWellDrawnObjects());
	}

	@Override
	protected void loadInitial(IValueObject inputObject) {
		Plate plate = SelectionUtils.getAsClass(inputObject, Plate.class);

		// Initialize a cell measurer
		try {
			measurer = new CellMeasurer(plate);
		} catch (IOException e) {
			EclipseLog.error("Cannot initialize cell measurer for plate " + plate, e, Activator.getDefault());
		}

		// Load the feature lists
		ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
		allFeatures = pClass.getSubWellFeatures().toArray(new SubWellFeature[0]);
		Arrays.sort(allFeatures, ProtocolUtils.FEATURE_NAME_SORTER);
		classificationFeatures = Arrays.stream(allFeatures).filter(f -> !f.getFeatureClasses().isEmpty()).toArray(i -> new SubWellFeature[i]);

		intensityFeatures = new FeatureMappings(pClass);

		if (allFeatures.length > 0) {
			if (xFeature == null) xFeature = allFeatures[0];
			if (yFeature == null) yFeature = allFeatures[0];
			if (sizeFeature == null) sizeFeature = allFeatures[0];
		}

		if (classificationFeatures.length > 0) {
			if (classificationFeature == null) classificationFeature = classificationFeatures[0];
			currentClass = classificationFeature.getFeatureClasses().get(0);
		}
	}

	private void loadInitialUI() {
		xFeatureCmb.setInput(allFeatures);
		xFeatureCmb.getCombo().select(CollectionUtils.find(allFeatures, xFeature));

		yFeatureCmb.setInput(allFeatures);
		yFeatureCmb.getCombo().select(CollectionUtils.find(allFeatures, yFeature));

		sizeCmb.setInput(allFeatures);
		sizeCmb.getCombo().select(CollectionUtils.find(allFeatures, sizeFeature));

		classificationFeatureCmb.setInput(classificationFeatures);
		classificationFeatureCmb.getCombo().select(CollectionUtils.find(classificationFeatures, classificationFeature));

		classTableViewer.setInput(classificationFeature);
		classTableViewer.setSelection(new StructuredSelection(currentClass));
	}

	@Override
	public void add(PathData path, ISelection selection) {
		super.add(path, selection);
		cellsViewer.refresh();
	}

	@Override
	protected AbstractDrawnObject generateObject(PathData path, ISelection selection) {
		DrawnCell cell = new DrawnCell();
		cell.path = path;
		cell.color = regionColor;
		try {
			cell.meas = measurer.measure(getCurrentWell(), path);
		} catch (IOException e) {
			throw new RuntimeException("Failed to measure cell", e);
		}
		return cell;
	}

	@Override
	protected void undo() {
		super.undo();
		cellsViewer.refresh();
	}

	@Override
	protected void reset() {
		super.reset();
		cellsViewer.setInput(getCurrentWellDrawnObjects());
	}

	@Override
	protected void doSave(IProgressMonitor monitor) throws IOException {
		Map<SubWellFeature, Map<Well, Object>> modifiedData = new HashMap<>();
		Set<Well> modifiedWells = getDrawnObjects().keySet();
		monitor.beginTask("Updating subwell data", modifiedWells.size()*2);

		if (classificationFeature != null) modifiedData.put(classificationFeature, new HashMap<>());
		modifiedData.put(xFeature, new HashMap<>());
		modifiedData.put(yFeature, new HashMap<>());
		modifiedData.put(sizeFeature, new HashMap<>());

		for (Well well: modifiedWells) {
			monitor.subTask("Processing well " + well);
			List<AbstractDrawnObject> objects = getDrawnObjects().get(well);
			if (objects == null || objects.isEmpty()) continue;

			if (classificationFeature != null && currentClass != null) {
				modifiedData.get(classificationFeature).put(well, updateData(well, classificationFeature, null, i -> {
					String pattern = currentClass.getPattern().replace(".", "0");
					int fClassValue = Integer.parseInt(pattern, 2);
					return Float.valueOf(fClassValue);
				}));
			}

			modifiedData.get(xFeature).put(well, updateData(well, xFeature, null, i -> ((DrawnCell)objects.get(i)).meas.cogX));
			modifiedData.get(yFeature).put(well, updateData(well, yFeature, null, i -> ((DrawnCell)objects.get(i)).meas.cogY));
			modifiedData.get(sizeFeature).put(well, updateData(well, sizeFeature, null, i -> ((DrawnCell)objects.get(i)).meas.area));

			CellMeasurement sampleMeas = ((DrawnCell)objects.get(0)).meas;
			for (FeatureMapping mapping: intensityFeatures.mappings.values()) {
				SubWellFeature f = (SubWellFeature)mapping.feature;
				if (f == null) continue;

				int chIndex = mapping.channel - 1;
				if (chIndex >= sampleMeas.avgIntensities.length) continue;

				if (modifiedData.get(f) == null) modifiedData.put(f, new HashMap<>());
				if (mapping.stat == "Avg") {
					modifiedData.get(f).put(well, updateData(well, f, null, i -> ((DrawnCell)objects.get(i)).meas.avgIntensities[chIndex]));
				} else if (mapping.stat == "Max") {
					modifiedData.get(f).put(well, updateData(well, f, null, i -> ((DrawnCell)objects.get(i)).meas.maxIntensities[chIndex]));
				} else if (mapping.stat == "Tot") {
					modifiedData.get(f).put(well, updateData(well, f, null, i -> ((DrawnCell)objects.get(i)).meas.totIntensities[chIndex]));
				}
			}

			monitor.worked(1);
		}

		monitor.subTask("Uploading data to server");
		SubWellService.getInstance().updateData(modifiedData);
		monitor.worked(modifiedWells.size());

		monitor.subTask("Recalculating plate");
		CalculationService.getInstance().triggerSubWellCalculation(getCurrentWell().getPlate());
		monitor.done();

		// Notify the others that subwell data has changed.
		ModelEvent event = new ModelEvent(modifiedWells.toArray(), ModelEventType.ObjectChanged, 0);
		ModelEventService.getInstance().fireEvent(event);
	}

	private Object updateData(Well well, SubWellFeature f, IntFunction<String> stringDataMapper, IntToDoubleFunction numericDataMapper) {
		if (well == null || f == null) return null;

		List<AbstractDrawnObject> newObjects = getDrawnObjects().get(well);
		int newObjectCount = (newObjects == null) ? 0 : newObjects.size();

		Object data = getExistingData(well, f);
		float[] numericData = (data instanceof float[]) ? (float[])data : null;
		String[] stringData = (data instanceof String[]) ? (String[])data : null;

		for (int i=0; i<newObjectCount; i++) {
			if (numericData != null) {
				numericData[(numericData.length - newObjectCount) + i] = (float)numericDataMapper.applyAsDouble(i);
			} else {
				stringData[(stringData.length - newObjectCount) + i] = stringDataMapper.apply(i);
			}
		}
		return data;
	}

	private Object getExistingData(Well well, SubWellFeature f) {
		List<AbstractDrawnObject> newObjects = getDrawnObjects().get(well);
		int newObjectCount = (newObjects == null) ? 0 : newObjects.size();

		if (f.isNumeric()) {
			float[] numericValues = SubWellService.getInstance().getNumericData(well, f);
			if (numericValues == null) numericValues = new float[0];
			int oldSize = numericValues.length;
			int newSize = oldSize + newObjectCount;
			numericValues = Arrays.copyOf(numericValues, newSize);
			Arrays.fill(numericValues, oldSize, newSize, Float.NaN);
			return numericValues;
		} else {
			String[] stringValues = SubWellService.getInstance().getStringData(well, f);
			if (stringValues == null) stringValues = new String[0];
			stringValues = Arrays.copyOf(stringValues, stringValues.length + newObjectCount);
			return stringValues;
		}
	}

	private ColumnConfiguration[] configureClassColumns() {

		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Class", "getLabel", DataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Symbol", DataType.String, 60);
		config.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void paint(Event event, Object element) {
				GC gc = event.gc;
				drawShape((FeatureClass)element, gc, event.x, event.y, gc.getClipping().width, event.height);
			}
			@Override
			protected void measure(Event event, Object element) {
				// Do nothing
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 150);
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private void drawShape(FeatureClass clazz, GC gc, int x, int y, int w, int h) {
		String shape = clazz.getSymbol();
		if (shape == null || shape.isEmpty()) return;
		int color = clazz.getRgbColor();
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setAntialias(SWT.ON);
		PlotShape ps = PlotShape.valueOf(shape);
		gc.setForeground(colorStore.get(new RGB((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff)));
		gc.setBackground(colorStore.get(new RGB((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff)));
		ps.drawShape(gc, x+ w/2, y+ h/2, 5, true);
	}

	@Override
	public void start() {
		if (!startBtn.getSelection()) return;
		super.start();
	}

	@Override
	public void cancel() {
		startBtn.setSelection(false);
		super.cancel();
	}

	private static class DrawnCell extends AbstractDrawnObject {
		public CellMeasurement meas;
	}

	public static class FeatureMappings {
		public ProtocolClass pClass;
		public Map<String, FeatureMapping> mappings;
		public ImageChannel[] channels;

		public FeatureMappings(ProtocolClass pClass) {
			this.pClass = pClass;
			this.mappings = new HashMap<>();

			channels = new ArrayList<>(pClass.getImageSettings().getImageChannels()).stream()
					.filter(ch -> ch.getType() == ImageChannel.CHANNEL_TYPE_RAW)
					.toArray(i -> new ImageChannel[i]);
			String[] stats = new String[] { "Avg", "Max", "Tot" };

			for (int c=1; c<=channels.length; c++) {
				for (String stat: stats) {
					SubWellFeature feature = findFeature(c, stat, pClass);
					mappings.put(c + stat, new FeatureMapping(c, stat, feature));
				}
			}
		}

		public void load(IMemento memento) {
			for (FeatureMapping fm: mappings.values()) {
				SubWellFeature f = PaletteStateHelper.getSubWellFeature(memento, "featureCh" + fm.channel + fm.stat + "Int");
				if (f != null) fm.feature = f;
			}
		}

		public void save(IMemento memento) {
			for (FeatureMapping fm: mappings.values()) {
				PaletteStateHelper.saveSubWellFeature((SubWellFeature)fm.feature, memento, "featureCh" + fm.channel + fm.stat + "Int");
			}
		}

		private static SubWellFeature findFeature(int channel, String stat, ProtocolClass pClass) {
			String regex = "[Cc]h" + channel + " ?" + stat +"Intensity";
			Pattern pattern = Pattern.compile(regex);
			for (SubWellFeature f: pClass.getSubWellFeatures()) {
				Matcher matcher = pattern.matcher(f.getName());
				if (matcher.matches()) {
					return f;
				}
			}
			return null;
		}
	}

	public static class FeatureMapping {
		public int channel;
		public String stat;
		public IFeature feature;

		public FeatureMapping(int channel, String stat, IFeature feature) {
			this.channel = channel;
			this.stat = stat;
			this.feature = feature;
		}

		public int getChannel() {
			return channel;
		}

		public String getStat() {
			return stat;
		}

		public IFeature getFeature() {
			return feature;
		}
	}
}
