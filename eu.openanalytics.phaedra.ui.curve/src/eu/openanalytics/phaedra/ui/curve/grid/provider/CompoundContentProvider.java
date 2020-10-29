package eu.openanalytics.phaedra.ui.curve.grid.provider;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.util.DispOptConstants;
import chemaxon.struc.Molecule;
import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.CensoredValueDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionDataColumnAccessor;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.Activator;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.curve.util.CurveComparators;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.MultiploCompound;
import eu.openanalytics.phaedra.ui.curve.grid.GridColumnGroup;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class CompoundContentProvider extends RichColumnAccessor<CompoundWithGrouping> implements ISelectionDataColumnAccessor<CompoundWithGrouping> {

	static String getBarcodes(CompoundWithGrouping gridCompound) {
		MultiploCompound multiploCompound = CompoundGridInput.getMultiploCompound(gridCompound);
		if (multiploCompound != null) {
			return multiploCompound.getCompounds().stream()
					.map((compound) -> compound.getPlate().getBarcode())
					.collect(Collectors.joining(", "));
		}
		else {
			return gridCompound.getPlate().getBarcode();
		}
	}
	
	static int getSampleCount(CompoundWithGrouping gridCompound) {
		MultiploCompound multiploCompound = CompoundGridInput.getMultiploCompound(gridCompound);
		if (multiploCompound != null) {
			return multiploCompound.getSampleCount();
		}
		else {
			return gridCompound.getWells().size();
		}
	}
	
	// PHA-651: 
	static String getConcentrationUnit(CompoundWithGrouping gridCompound) {
		return DataTypePrefs.getDefaultConcentrationUnit().toString();
	}

	private static final String CURVE_HEIGHT = "curveHeight";
	private static final String CURVE_WIDTH = "curveWidth";

	private CompoundGridInput gridInput;
	
	private final DataFormatSupport dataFormatSupport;

	private ColumnSpec[] columnSpecs;
	private GridColumnGroup[] columnGroups;
	
	private int imageX = 100;
	private int imageY = 100;

	private DataPreLoader preLoader;
	private Map<Compound, ImageData> smilesImages;
	
	private int baseColumnCount;
	private int structureColumnIndex;
	
	public CompoundContentProvider(CompoundGridInput gridInput, DataFormatSupport dataFormatSupport) {
		this.gridInput = gridInput;

		this.dataFormatSupport = dataFormatSupport;
		this.dataFormatSupport.addListener(this::updateLabels);
		
		this.smilesImages = new HashMap<>();

		List<ColumnSpec> columnSpecList = new ArrayList<>();
		columnSpecList.add(new ColumnSpec("Experiment", null, 110, null, c -> c.getPlate().getExperiment().getName()));
		columnSpecList.add(new ColumnSpec("Plate(s)", null, 85, null, CompoundContentProvider::getBarcodes));
		columnSpecList.add(new ColumnSpec("PV", "Plate Validation Status", 35, null, c -> c.getPlate().getValidationStatus(),
				c -> PlateValidationStatus.getByCode(c.getPlate().getValidationStatus()).toString(), false));
		columnSpecList.add(new ColumnSpec("CV", "Compound Validation Status", 35, null, c -> c.getValidationStatus(),
				c -> CompoundValidationStatus.getByCode(c.getValidationStatus()).toString(), false));
		columnSpecList.add(new ColumnSpec("Comp.Type", null, 65, null, c -> c.getType()));
		columnSpecList.add(new ColumnSpec("Comp.Nr", null, 60, null, c -> c.getNumber()));
		columnSpecList.add(new ColumnSpec("Saltform", null, 90, null, c -> c.getSaltform()));
		columnSpecList.add(new ColumnSpec("Grouping", null, 70, null, c -> c.getGrouping()));
		columnSpecList.add(new ColumnSpec("Samples", null, 90, null, CompoundContentProvider::getSampleCount));
		// PHA-651: 
		columnSpecList.add(new ColumnSpec("Concentration Unit",  "Concentration Unit",  150,  null, CompoundContentProvider::getConcentrationUnit));
		columnSpecList.add(new ColumnSpec("Smiles", null, -1, null, c -> {
			if (smilesImages.containsKey(c)) return smilesImages.get(c);
			ImageData img = makeSmilesImage(c);
			smilesImages.put(c, img);
			return img;
		}));
		baseColumnCount = columnSpecList.size();
		structureColumnIndex = baseColumnCount - 1;
		
		List<GridColumnGroup> columnGroupList = new ArrayList<>();
		for (Feature feature: gridInput.getFeatures()) {
			CurveFitSettings curveSettings = CurveFitService.getInstance().getSettings(feature);
			if (curveSettings == null) continue;
			ICurveFitModel model = CurveFitService.getInstance().getModel(curveSettings.getModelId());
			if (model == null) continue;
			
			List<Definition> params = model.getOutputKeyParameters();
			for (Definition param: params) {
				columnSpecList.add(new ColumnSpec(param, 60, feature, c -> {
					Curve curve = getCurve(c, feature);
					if (curve == null) return null;
					Value value = CurveParameter.find(curve.getOutputParameters(), param.name);
					return CurveParameter.renderValue(value, curve, this.dataFormatSupport.get());
				}));
			}
			columnSpecList.add(new ColumnSpec("Curve", null, 100, feature, c -> {
				Curve curve = getCurve(c, feature);
				if (curve == null) return null;
				return CurveFitService.getInstance().getCurveImage(curve.getId(), imageX, imageY);
			}));
			
			//TODO eMax is an OSB-specific parameter.
			columnSpecList.add(new ColumnSpec("eMax Image", null, 100, feature, c -> getEMaxImage(feature, c), null, true));
			
			int indexStart = columnSpecList.size() - (params.size() + 2);
			columnGroupList.add(new GridColumnGroup(feature.getDisplayName(), IntStream.range(indexStart, columnSpecList.size()).toArray()));
		}
		
		columnSpecs = columnSpecList.toArray(new ColumnSpec[columnSpecList.size()]);
		columnGroups = columnGroupList.toArray(new GridColumnGroup[columnGroupList.size()]);
		updateLabels();
	}

	public void preLoad(NatTable table) {
		if (preLoader != null) preLoader.cancel();
		preLoader = new DataPreLoader(table);
		preLoader.setUser(true);
		preLoader.schedule();
	}

	private void updateLabels() {
		if (columnSpecs == null) {
			return;
		}
		DataFormatter dataFormatter = this.dataFormatSupport.get();
		for (int i = 0; i < columnSpecs.length; i++) {
			Definition def = columnSpecs[i].paramDefinition;
			if (def != null) {
				columnSpecs[i].label = def.getDataDescription().convertNameTo(columnSpecs[i].name, dataFormatter);
			}
		}
	}
	

	public void setImageSize(int x, int y) {
		imageX = x;
		imageY = y;
	}

	public int getImageWidth() {
		return imageX;
	}

	public int getImageHeight() {
		return imageY;
	}

	public int[] getCurveColumns() {
		return IntStream.range(0, columnSpecs.length).filter(i -> columnSpecs[i].name.equals("Curve")).toArray();
	}
	
	public int[] getImageColumns() {
		return IntStream.range(0, columnSpecs.length).filter(i -> 
			columnSpecs[i].name.equals("Curve") || columnSpecs[i].name.contains("Image")).toArray();
	}

	public int getStructureColumn() {
		return structureColumnIndex;
	}

	@Override
	public Object getDataValue(CompoundWithGrouping c, int columnIndex) {
		ColumnSpec spec = columnSpecs[columnIndex];
		if (spec.valueRenderer != null) return spec.valueRenderer.apply(c);
		return null;
	}

	@Override
	public Object getSelectionValue(CompoundWithGrouping c, int column) {
		if (column < baseColumnCount) return c;
		else return getCurve(c, columnSpecs[column].feature);
	}

	@Override
	public int getColumnCount() {
		return columnSpecs.length;
	}

	@Override
	public String getColumnProperty(int columnIndex) {
		return columnSpecs[columnIndex].label;
	}

	@Override
	public int getColumnIndex(String propertyName) {
		for (int i = 0; i < columnSpecs.length; i++) {
			if (columnSpecs[i].name.equals(propertyName) || columnSpecs[i].label.equals(propertyName)) return i;
		}
		return -1;
	}

	@Override
	public String getTooltipText(CompoundWithGrouping rowObject, int colIndex) {
		if (rowObject == null) return columnSpecs[colIndex].tooltip;
		if (columnSpecs[colIndex].tooltipRenderer != null) return columnSpecs[colIndex].tooltipRenderer.apply(rowObject);
		return null;
	}

	@Override
	public int[] getColumnWidths() {
		return Arrays.stream(columnSpecs).mapToInt(s -> s.width).toArray();
	}

	@Override
	public Map<int[], AbstractCellPainter> getCustomCellPainters() {
		Map<int[], AbstractCellPainter> painters = new HashMap<>();
		painters.put(new int[] { 1 }, new TextPainter(true, true));
		painters.put(new int[] { 2 }, new FlagCellPainter("plate",
				new FlagMapping(FlagFilter.Negative, Flag.Red),
				new FlagMapping(FlagFilter.Zero, Flag.White),
				new FlagMapping(FlagFilter.Positive, Flag.Green)
		));
		painters.put(new int[] { 3 }, new FlagCellPainter("curve",
				new FlagMapping(FlagFilter.Negative, Flag.Red),
				new FlagMapping(FlagFilter.Zero, Flag.White),
				new FlagMapping(FlagFilter.One, Flag.Blue),
				new FlagMapping(FlagFilter.GreaterThanOne, Flag.Green)
		));
		return painters;
	}

	@Override
	public IConfiguration getCustomConfiguration() {
		return new AbstractRegistryConfiguration() {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				for (int i = 0; i < columnSpecs.length; i++) {
					Comparator<String> comp = null;
					Definition def = columnSpecs[i].paramDefinition;
					if (def != null && def.type.isNumeric()) {
						if (def.getDataDescription() instanceof CensoredValueDescription) {
							updateLabels();
							comp = CurveComparators.CENSOR_COMPARATOR;
						} else if (def.type.isNumeric()) 
							comp = CurveComparators.NUMERIC_STRING_COMPARATOR;
					}
					
					if (comp != null) {
						configRegistry.registerConfigAttribute(
								SortConfigAttributes.SORT_COMPARATOR
								, comp
								, DisplayMode.NORMAL
								, columnSpecs[i].label);
						configRegistry.registerConfigAttribute(
								FilterRowConfigAttributes.FILTER_COMPARATOR
								, comp
								, DisplayMode.NORMAL
								, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + i
						);
					}
				}

				Function<Object, String> mapper = t -> {
					if (t instanceof EntityStatus) return ((EntityStatus) t).getCode()+"";
					else return t.toString();
				};

				NatTableUtils.applyAdvancedComboFilter(configRegistry, 2, Arrays.asList(PlateValidationStatus.values())
						, new DefaultDisplayConverter()
						, new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								return ((List<?>) canonicalValue).stream().map(Object::toString).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						}), new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								// Manually typed expressions will return List<String>, Combobox List<EntityStatus.getCode()>.
								return ((List<?>) canonicalValue).stream().map(mapper).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						})
				);

				NatTableUtils.applyAdvancedComboFilter(configRegistry, 3, Arrays.asList(CompoundValidationStatus.values())
						, new DefaultDisplayConverter()
						, new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								// Manually typed expressions will return List<String>, Combobox List<PlateValidationStatus>.
								return ((List<?>) canonicalValue).stream().map(Object::toString).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						}), new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								// Manually typed expressions will return List<String>, Combobox List<WellStatus>.
								return ((List<?>) canonicalValue).stream().map(mapper).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						})
				);
			}
		};
	}

	public GridColumnGroup[] getGroups() {
		return columnGroups;
	}

	public List<Integer> getDefaultHiddenColumns() {
		List<Integer> indices = new ArrayList<Integer>();
		indices.add(getStructureColumn());
		for (int i=0; i<columnSpecs.length; i++) {
			if (columnSpecs[i].defaultHidden) indices.add(i);
		}
		return indices;
	}

	public void saveSettings(Properties properties) {
		properties.addProperty(CURVE_WIDTH, getImageWidth());
		properties.addProperty(CURVE_HEIGHT, getImageHeight());
	}

	public void loadSettings(Properties properties) {
		int curveWidth = properties.getProperty(CURVE_WIDTH, getImageWidth());
		int curveHeight = properties.getProperty(CURVE_HEIGHT, getImageHeight());
		setImageSize(curveWidth, curveHeight);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private Curve getCurve(CompoundWithGrouping gridCompound, Feature f) {
		return CurveFitService.getInstance().getCurve(gridCompound, f, gridCompound.getGrouping(), true);
	}
	
	private ImageData makeSmilesImage(Compound c) {
		String smiles = CompoundInfoService.getInstance().getInfo(c).getSmiles();

		Image img = null;
		try {
			Molecule mol = MolImporter.importMol(smiles);
			if (mol != null) {
				BufferedImage im = new BufferedImage(imageX, imageY, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = im.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, im.getWidth(), im.getHeight());
				mol.draw(g, "w" + imageX + ",h" + imageY + DispOptConstants.RENDERING_STYLES[DispOptConstants.STICKS]);
				img = AWTImageConverter.convert(null, im);
				img = ImageUtils.addTransparency(img, 0xFFFFFF);
				return img.getImageData();
			}
		} catch (MolFormatException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			if (img != null) img.dispose();
		}
		return null;
	}

	private ImageData getEMaxImage(Feature feature, CompoundWithGrouping c) {
		Function<Well, Double> fvMapper = w -> CalculationService.getInstance()
				.getAccessor(w.getPlate())
				.getNumericValue(w, feature, feature.getNormalization());
		Well eMaxWell = new ArrayList<>(c.getWells()).stream().max((w1,w2) -> Double.compare(fvMapper.apply(w1), fvMapper.apply(w2))).orElse(null);
		if (eMaxWell == null) return null;
		
		List<ImageChannel> channels = ProtocolUtils.getProtocolClass(eMaxWell).getImageSettings().getImageChannels();
		boolean[] channelFilter = new boolean[channels.size()];
		for (int j = 0; j < channelFilter.length; j++) channelFilter[j] = channels.get(j).isShowInPlateView();
		
		try {
			return ImageRenderService.getInstance().getWellImageData(eMaxWell, imageX, imageY, channelFilter);
		} catch (IOException e) {
			return null;
		}
	}
	
	private class DataPreLoader extends Job {

		private NatTable table;

		public DataPreLoader(NatTable table) {
			super("Loading Curve Data");
			this.table = table;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<CompoundWithGrouping> compounds = gridInput.getGridCompounds();
			List<Feature> features = gridInput.getFeatures();
			
			int curveCount = Math.min(compounds.size() * features.size(), 100000);
			monitor.beginTask("Loading Curves", curveCount);

			ThreadPool tp = new ThreadPool(3);
			try {
				AtomicInteger curveLoadedCount = new AtomicInteger(0);

				ThreadUtils.runQuery(() -> {
					compounds.parallelStream().forEach(c -> {
						if (monitor.isCanceled()) return;
						if (table != null && table.isDisposed()) return;

						// There's a limit to the number of curves we can (should) pre-load, load no more.
						if (curveLoadedCount.get() >= curveCount) return;

						for (Feature f: features) {
							if (monitor.isCanceled()) return;
							monitor.subTask("Loading Curves: " + curveLoadedCount + "/" + curveCount);
							if (f == null || c == null)	System.out.println("Feature " + f + ", Cruve " + c);
							final Curve curve = getCurve(c, f);
							curveLoadedCount.addAndGet(1);
							if (curve == null) continue;
							
							// Send the render task to another thread, so this thread can keep loading curves.
							//FIX Ubuntu/Cairo: new Image() may cause deadlock when called from non-UI thread.
							Runnable curveImageGetter = () -> CurveFitService.getInstance().getCurveImage(curve.getId(), imageX, imageY);
							if (ProcessUtils.isWindows()) tp.schedule(curveImageGetter);
							else Display.getDefault().asyncExec(curveImageGetter);

							monitor.worked(1);
							if (curveLoadedCount.get() >= curveCount) break;
						}

						// For the first few curves (which are on-screen), refresh the table.
						if (curveLoadedCount.get() / features.size() < 10) table.doCommand(new VisualRefreshCommand());
					});
				});
			} catch (Exception e) {
				// Failed to pre-load curves: those curves will not be cached.
				EclipseLog.warn(e.getMessage(), e, Activator.getDefault());
			} finally {
				tp.stop(true);
			}

			if (monitor.isCanceled() || (table != null && table.isDisposed())) return Status.CANCEL_STATUS;

			monitor.done();
			return Status.OK_STATUS;
		}

	}

	private static class ColumnSpec {
		public String name;
		/** decorated name */ private String label;
		public String tooltip;
		public int width;
		public Feature feature;
		public Definition paramDefinition;
		public Function<CompoundWithGrouping, Object> valueRenderer;
		public Function<CompoundWithGrouping, String> tooltipRenderer;
		public boolean defaultHidden;
		
		public ColumnSpec(String name, String tooltip, int width, Feature feature, Function<CompoundWithGrouping, Object> valueRenderer) {
			this(name, tooltip, width, feature, valueRenderer, null, false);
		}
		
		public ColumnSpec(String name, String tooltip, int width, Feature feature,
				Function<CompoundWithGrouping, Object> valueRenderer, Function<CompoundWithGrouping, String> tooltipRenderer, boolean defaultHidden) {
			super();
			this.name = name;
			this.label = name;
			this.tooltip = tooltip;
			this.width = width;
			this.feature = feature;
			this.paramDefinition = null;
			this.valueRenderer = valueRenderer;
			this.tooltipRenderer = tooltipRenderer;
			this.defaultHidden = defaultHidden;
		}
		
		public ColumnSpec(Definition paramDefinition, int width, Feature feature, Function<CompoundWithGrouping, Object> valueRenderer) {
			this.name = paramDefinition.name;
			this.label = name;
			this.tooltip = paramDefinition.description;
			this.width = width;
			this.feature = feature;
			this.paramDefinition = paramDefinition;
			this.valueRenderer = valueRenderer;
			this.tooltipRenderer = null;
			this.defaultHidden = false;
		}
		
	}
}
