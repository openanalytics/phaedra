package eu.openanalytics.phaedra.ui.plate.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;
import eu.openanalytics.phaedra.base.ui.nattable.misc.AsyncColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IAsyncColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.ILinkedColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.AsyncWellDataAccessor;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.WellDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellDataCalculator implements ILinkedColumnAccessor<Well>, IRichColumnAccessor<Well>, IAsyncColumnAccessor<Well> {

	public static final int IMAGE_COLUMN_INDEX = 0;

	private static final String CONCENTRATION = "Concentration";
	// PHA-651: UR-007: Add conc units to table view, compound browser and DRC View
	private static final String CONCENTRATION_UNIT = "Concentration Unit";
	private static final String COMPOUND_TYPE = "Compound Type";
	private static final String COMPOUND_NR = "Compound";
	private static final String DESCRIPTION = "Description";
	public static final String WELL_STATUS = "V";
	private static final String WELL_TYPE = "Well Type";
	private static final String COLUMN = "Col";
	private static final String ROW = "Row";
	private static final String WELL_NR = "Well Nr";
	private static final String PLATE = "Plate";
	private static final String IMAGE = "Image";

	private static final String[] COLUMNS = new String[] { IMAGE, PLATE, WELL_NR, ROW, COLUMN, WELL_TYPE, WELL_STATUS
		, DESCRIPTION, COMPOUND_TYPE, COMPOUND_NR, CONCENTRATION, CONCENTRATION_UNIT };
	private static final String[] COLUMNS_TOOLTIPS = new String[] { IMAGE, PLATE, WELL_NR, ROW, "Column", WELL_TYPE, "Well Validation Status"
		, DESCRIPTION, COMPOUND_TYPE, COMPOUND_NR, CONCENTRATION, CONCENTRATION_UNIT };
	
	
	private final Supplier<? extends DataUnitConfig> dataUnitSupplier;
	
	private List<Well> currentWells;
	private List<Feature> features;
	private ImageAsyncColumnAccessor imageAccessor;
	private AsyncWellDataAccessor wellDataAccessor;

	private float scale;
	private int imgW;
	private int imgH;
	private boolean[] channels;

	private java.util.Properties tableProperties;

	private List<FeatureTextPainter> painterList;

	private boolean isMultiPlate;
	private boolean isAsync;
	/** Contains the Features that already have been fully loaded when in sync. */
	private Set<Feature> featuresSyncLoaded;
	
	
	public WellDataCalculator(Supplier<? extends DataUnitConfig> dataUnitSupport) {
		this(false, dataUnitSupport);
	}

	public WellDataCalculator(boolean isMultiPlate, Supplier<? extends DataUnitConfig> dataUnitSupport) {
		this.dataUnitSupplier = dataUnitSupport; 
		this.scale = 1f/32;
		this.features = new ArrayList<>();
		this.currentWells = new ArrayList<>();
		this.painterList = new ArrayList<>();
		this.imageAccessor = new ImageAsyncColumnAccessor();
		this.wellDataAccessor = new AsyncWellDataAccessor();
		this.isMultiPlate = isMultiPlate;
		this.isAsync = true;
		this.featuresSyncLoaded = new HashSet<>();
	}

	@Override
	public void setAsync(boolean isAsync) {
		this.isAsync = isAsync;
		this.featuresSyncLoaded.clear();
		// Delegate to ImageAccessor.
		this.imageAccessor.setAsync(isAsync);
	}

	public void dispose() {
		wellDataAccessor.dispose();
		imageAccessor.dispose();
		for (FeatureTextPainter painter : painterList) {
			painter.dispose();
		}
	}

	public void clearCache() {
		imageAccessor.reset();
	}

	public void setTable(NatTable table) {
		imageAccessor.setTable(table);
		wellDataAccessor.setRefresher(() -> {
			if (table != null && !table.isDisposed()) {
				table.doCommand(new VisualRefreshCommand());
			}
		});
	}

	public Point getWellImageSize(Well well) {
		return ImageRenderService.getInstance().getWellImageSize(well, scale);
	}

	public List<Well> getCurrentWells() {
		return currentWells;
	}

	public void setCurrentWells(List<Well> currentWells) {
		this.currentWells = currentWells;
		if (isMultiPlate) wellDataAccessor.preload(currentWells, features);
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
		if (isMultiPlate) wellDataAccessor.preload(currentWells, features);
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public Point getImageSize() {
		return new Point(imgW, imgH);
	}

	public void setImageSize(int w, int h) {
		this.imgW = w;
		this.imgH = h;
		this.imageAccessor.reset();
	}

	public boolean[] getChannels() {
		return channels;
	}

	public void setChannels(boolean[] channels) {
		this.channels = channels;
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length + features.size();
	}

	@Override
	public Object getDataValue(Well well, int columnIndex) {
		if (columnIndex < COLUMNS.length) {
			switch (COLUMNS[columnIndex]) {
			case IMAGE:
				return imageAccessor.getDataValue(well, columnIndex);
			case WELL_NR:
				return NumberUtils.getWellCoordinate(well.getRow(), well.getColumn());
			case ROW:
				return well.getRow();
			case COLUMN:
				return well.getColumn();
			case WELL_TYPE:
				return ProtocolUtils.getCustomHCLCLabel(well.getWellType()); // PHA-644
			case WELL_STATUS:
				return well.getStatus();
			case DESCRIPTION:
				return well.getDescription();
			case COMPOUND_TYPE:
				Compound c = well.getCompound();
				return (c == null) ? "" : c.getType();
			case COMPOUND_NR:
				c = well.getCompound();
				return (c == null) ? "" : c.getNumber();
			case CONCENTRATION:
				// PHA-651: UR-007: Add conc units to table view, compound browser and DRC View
				double temp = NumberUtils.roundUp(WellProperty.Concentration.getRealValue(well, dataUnitSupplier.get()), DataTypePrefs.getDefaultConcentrationFormatDigits());
//				return WellProperty.Concentration.getRealValue(well, dataUnitSupplier.get());
				return temp;
			// PHA-651: UR-007: Add conc units to table view, compound browser and DRC View
			case CONCENTRATION_UNIT:
				return DataTypePrefs.getDefaultConcentrationUnit();
			case PLATE:
				return well.getPlate().getBarcode();
			default:
				return null;
			}
		} else {
			Feature f = features.get(columnIndex - COLUMNS.length);
			if (isMultiPlate) {
				if (!isAsync) {
					// Async is disabled, load the whole column.
					if (!featuresSyncLoaded.contains(f)) {
						WellDataAccessor.fetchFeatureValues(currentWells, f, true);
						featuresSyncLoaded.add(f);
					}
				}
				return wellDataAccessor.getObjectValue(well, f);
			} else {
				PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
				if (f.isNumeric()) return accessor.getNumericValue(well, f, f.getNormalization());
				else return accessor.getStringValue(well, f);
			}
		}
	}
	
	@Override
	public void setDataValue(Well rowObject, int columnIndex, Object newValue) {
		// Not supported.
	}

	@Override
	public String getColumnProperty(int columnIndex) {
		if (columnIndex < COLUMNS.length) return COLUMNS[columnIndex];
		else return features.get(columnIndex - COLUMNS.length).getDisplayName();
	}

	@Override
	public int getColumnIndex(String propertyName) {
		int index = CollectionUtils.find(COLUMNS, propertyName);
		if (index < 0) {
			index = 0;
			for ( ; index < features.size(); index++) {
				IFeature f = features.get(index);
				if (f.getDisplayName().equalsIgnoreCase(propertyName)) {
					return index + COLUMNS.length;
				}
			}
		}
		return index;
	}

	@Override
	public String getTooltipText(Well rowObject, int colIndex) {
		if (rowObject == null) {
			if (colIndex < COLUMNS_TOOLTIPS.length) return COLUMNS_TOOLTIPS[colIndex];
			else if ((colIndex - COLUMNS.length) < features.size()) return features.get(colIndex - COLUMNS.length).getName();
		}

		if (rowObject != null) {
			if (colIndex == 6) return WellStatus.getByCode(rowObject.getStatus()).getDescription();
		}
		return null;
	}

	@Override
	public int[] getLinkedColumns() {
		return new int[] { IMAGE_COLUMN_INDEX };
	}

	@Override
	public int[] getColumnWidths() {
		int[] columnWidths = new int[getColumnCount()];
		Arrays.fill(columnWidths, -1);
		columnWidths[0] = 20;
		columnWidths[2] = 60;
		columnWidths[3] = 35;
		columnWidths[4] = 35;
		columnWidths[5] = 75;
		columnWidths[6] = 35;
		return columnWidths;
	}

	@Override
	public Map<int[], AbstractCellPainter> getCustomCellPainters() {
		Map<int[], AbstractCellPainter> painters = new HashMap<>();

		int[] flagColumns = new int[] { 6 };
		painters.put(flagColumns, new FlagCellPainter(new FlagMapping(FlagFilter.Negative, Flag.Red),
				new FlagMapping(FlagFilter.Zero, Flag.White), new FlagMapping(FlagFilter.Positive, Flag.Green)));

		List<Plate> plates = getPlates();
		if (!isMultiPlate) {
			for (int i = 0; i < features.size(); i++) {
				Feature f = features.get(i);
				if (f.isNumeric()) {
					int index = COLUMNS.length + i;
					FeatureTextPainter painter = new FeatureTextPainter(f, plates);
					painterList.add(painter);
					painters.put(new int[] { index }, painter);
				}
			}
		}

		return painters;
	}

	@Override
	public IConfiguration getCustomConfiguration() {
		return new AbstractRegistryConfiguration() {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				// Custom comparator for sorting numeric strings.
				configRegistry.registerConfigAttribute(
						SortConfigAttributes.SORT_COMPARATOR
						, (String s1, String s2) -> StringUtils.compareToNumericStrings(s1, s2)
						, DisplayMode.NORMAL
						, WellDataCalculator.WELL_NR
				);

				Function<Object, String> wellStatusToString = t -> {
					if (t instanceof WellStatus) return ((WellStatus) t).getLabel();
					else return t.toString();
				};
				Function<Object, String> wellStatusToCodeString = t -> {
					if (t instanceof EntityStatus) return ((EntityStatus) t).getCode()+"";
					else return t.toString();
				};

				Function<Object, Object> comboConverter = t -> {
					if (t instanceof WellStatus) return ((WellStatus) t).getLabel();
					return t.toString();
				};
				Function<Object, Object> filterCellConverter = canonicalValue -> {
					if (canonicalValue instanceof List) {
						// Manually typed expressions will return List<String>, Combobox List<WellStatus>.
						return ((List<?>) canonicalValue).stream().map(wellStatusToString).collect(Collectors.joining(", "));
					}
					return canonicalValue;
				};
				Function<Object, Object> canonicalToDisplay = canonicalValue -> {
					if (canonicalValue instanceof List) {
						// Manually typed expressions will return List<String>, Combobox List<EntityStatus.getCode()>.
						return ((List<?>) canonicalValue).stream().map(wellStatusToCodeString).collect(Collectors.joining(", "));
					}
					return canonicalValue;
				};
				NatTableUtils.applyAdvancedComboFilter(configRegistry, 6, Arrays.asList(WellStatus.values())
						, new FunctionDisplayConverter(comboConverter)
						, new FunctionDisplayConverter(filterCellConverter)
						, new FunctionDisplayConverter(canonicalToDisplay)
				);
			}
		};
	}

	@Override
	public Map<String, IColumnMatcher> getColumnDialogMatchers() {
		Map<String, IColumnMatcher> columnMatchers = new LinkedHashMap<>();

		columnMatchers.put("Select All", col -> true);
		columnMatchers.put("Select Key Features", col -> {
			int colIndex = col.getIndex() - COLUMNS.length;
			if (colIndex >= 0) return features.get(colIndex).isKey();
			return false;
		});
		columnMatchers.put("Select Numeric Features", col -> {
			int colIndex = col.getIndex() - COLUMNS.length;
			if (colIndex >= 0) return features.get(colIndex).isNumeric();
			return false;
		});

		return columnMatchers;
	}

	public float getImageAspectRatio(Plate plate) {
		return ImageRenderService.getInstance().getWellImageAspectRatio(plate);
	}

	public float getImageAspectRatio(List<Well> wells) {
		return ImageRenderService.getInstance().getWellImageAspectRatio(wells);
	}

	public void resetPainters() {
		for (FeatureTextPainter painter : painterList) {
			painter.resetColorMethod();
		}
	}

	private List<Plate> getPlates() {
		Set<Plate> plates = new HashSet<>();
		for (Well w : currentWells) {
			plates.add(w.getPlate());
		}
		return new ArrayList<>(plates);
	}

	public void loadState(NatTable table) {
		table.loadState("", getTableProperties());
	}

	public void saveState(NatTable table) {
		tableProperties = new java.util.Properties();
		table.saveState("", tableProperties);
	}

	private java.util.Properties getTableProperties() {
		if (tableProperties == null) {
			return new java.util.Properties();
		}
		return tableProperties;
	}

	private class ImageAsyncColumnAccessor extends AsyncColumnAccessor<Well> {

		@Override
		protected Object loadDataValue(Well well, int colIndex) {
			try {
				if (imgW > 0) {
					return ImageRenderService.getInstance().getWellImageData(well, imgW, imgH, getChannels());
				} else {
					return ImageRenderService.getInstance().getWellImageData(well, getScale(), getChannels());
				}
			} catch (IOException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				return null;
			}
		}

	}

}
