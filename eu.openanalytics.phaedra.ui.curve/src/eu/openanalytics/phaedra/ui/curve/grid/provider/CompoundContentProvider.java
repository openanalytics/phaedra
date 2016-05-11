package eu.openanalytics.phaedra.ui.curve.grid.provider;

import static eu.openanalytics.phaedra.model.curve.util.ConcentrationFormat.LogMolar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.util.DispOptConstants;
import chemaxon.struc.Molecule;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionDataColumnAccessor;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.model.curve.Activator;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.util.ConcentrationFormat;
import eu.openanalytics.phaedra.model.curve.util.CurveComparators;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.MultiploCompound;
import eu.openanalytics.phaedra.ui.curve.grid.GridColumnGroup;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class CompoundContentProvider extends RichColumnAccessor<Compound> implements ISelectionDataColumnAccessor<Compound> {

	private static final String CURVE_HEIGHT = "curveHeight";
	private static final String CURVE_WIDTH = "curveWidth";

	private List<Compound> compounds;
	private List<Feature> features;

	private String[] columnNames;
	private String[] columnTooltips;
	private GridColumnGroup[] columnGroups;

	private int imageX = 100;
	private int imageY = 100;

	private CurvePreLoader preLoader;
	private Map<Compound, ImageData> smilesImages;

	private ConcentrationFormat concFormat;

	private int baseColumnCount;
	private int featureColumnCount;
	private int structureColumnIndex;

	public CompoundContentProvider(List<Compound> compounds, List<Feature> features) {
		this.compounds = compounds;
		this.features = features;

		this.concFormat = ConcentrationFormat.LogMolar;
		this.smilesImages = new HashMap<>();

		baseColumnCount = 10;
		featureColumnCount = 6;
		structureColumnIndex = 9;

		columnNames = new String[(features.size()*featureColumnCount)+baseColumnCount];
		columnNames[0] = "Experiment";
		columnNames[1] = "Plate";
		columnNames[2] = "PV";
		columnNames[3] = "CV";
		columnNames[4] = "Comp.Type";
		columnNames[5] = "Comp.Nr";
		columnNames[6] = "Saltform";
		columnNames[7] = "Grouping";
		columnNames[8] = "Samples";
		columnNames[9] = "Smiles";

		for (int i=0; i<features.size(); i++) {
			String kind = features.get(i).getCurveSettings().get(CurveSettings.KIND);
			if (kind.equals("OSB")) columnNames[baseColumnCount+i*featureColumnCount] = "pIC50";
			else columnNames[baseColumnCount+i*featureColumnCount] = "pLAC";
			columnNames[baseColumnCount+1+i*featureColumnCount] = "EMax Conc";
			columnNames[baseColumnCount+2+i*featureColumnCount] = "EMax Effect";
			columnNames[baseColumnCount+3+i*featureColumnCount] = "R2";
			columnNames[baseColumnCount+4+i*featureColumnCount] = "Hill";
			columnNames[baseColumnCount+5+i*featureColumnCount] = "Curve";
		}

		columnTooltips = new String[columnNames.length];
		for (int i=0; i<columnNames.length; i++) {
			columnTooltips[i] = columnNames[i];
		}
		columnTooltips[2] = "Plate Validation Status";
		columnTooltips[3] = "Compound Validation Status";

		columnGroups = new GridColumnGroup[features.size()];
		for (int i=0; i<columnGroups.length; i++) {
			Feature f = features.get(i);
			String name = f.getDisplayName();
			int[] indices = new int[]{
					baseColumnCount+(i*featureColumnCount),
					baseColumnCount+1+(i*featureColumnCount),
					baseColumnCount+2+(i*featureColumnCount),
					baseColumnCount+3+(i*featureColumnCount),
					baseColumnCount+4+(i*featureColumnCount),
					baseColumnCount+5+(i*featureColumnCount)
			};
			columnGroups[i] = new GridColumnGroup(name, indices);
		}
	}

	public void preLoad(NatTable table) {
		if (preLoader != null) preLoader.cancel();
		preLoader = new CurvePreLoader(table);
		preLoader.setUser(true);
		preLoader.schedule();
	}

	public void setConcFormat(ConcentrationFormat concFormat) {
		this.concFormat = concFormat;
		for (int i=baseColumnCount; i<columnNames.length; i++) {
			int featureProp = (i - baseColumnCount) % featureColumnCount;
			if (featureProp < 2) columnNames[i] = concFormat.decorateName(columnNames[i]);
		}
	}

	public ConcentrationFormat getConcFormat() {
		return concFormat;
	}

	public void setCurveSize(int x, int y) {
		imageX = x;
		imageY = y;
	}

	public int getCurveWidth() {
		return imageX;
	}

	public int getCurveHeight() {
		return imageY;
	}

	public int[] getCurveColumns() {
		int curveColumnCount = (columnNames.length-baseColumnCount)/featureColumnCount;
		int[] indices = new int[curveColumnCount];
		for (int i=0; i<curveColumnCount; i++) {
			indices[i] = baseColumnCount+(featureColumnCount-1)+(i*featureColumnCount);
		}
		return indices;
	}

	public int getStructureColumn() {
		return structureColumnIndex;
	}

	@Override
	public Object getDataValue(Compound c, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return c.getPlate().getExperiment().getName();
		case 1:
			if (getMultiploCompound(c) != null) return "<Multiplo>";
			return c.getPlate().getBarcode();
		case 2:
			return c.getPlate().getValidationStatus();
		case 3:
			return c.getValidationStatus();
		case 4:
			return c.getType();
		case 5:
			return c.getNumber();
		case 6:
			return c.getSaltform();
		case 7:
			if (c instanceof CompoundWithGrouping) return ((CompoundWithGrouping)c).getGrouping();
			return "";
		case 8:
			if (getMultiploCompound(c) != null) return getMultiploCompound(c).getSampleCount();
			return c.getWells().size();
		case 9:
			if (smilesImages.containsKey(c)) return smilesImages.get(c);
			ImageData img = makeSmilesImage(c);
			smilesImages.put(c, img);
			return img;
		}

		Feature f = features.get((columnIndex-baseColumnCount)/featureColumnCount);
		Curve curve = getCurve(c, f);
		if (curve == null) return null;

		int mod = (columnIndex-baseColumnCount)%featureColumnCount;
		switch (mod) {
		case 0:
			if (curve instanceof OSBCurve) {
				OSBCurve osb = (OSBCurve)curve;
				return ConcentrationFormat.format(LogMolar, concFormat, osb.getPic50Censor(), osb.getPic50());
			} else if (curve instanceof PLACCurve) {
				PLACCurve plac = (PLACCurve)curve;
				return ConcentrationFormat.format(LogMolar, concFormat, plac.getPlacCensor(), plac.getPlac());
			}
		case 1:
			return ConcentrationFormat.format(LogMolar, concFormat, null, curve.geteMaxConc());
		case 2:
			return Formatters.getInstance().format(curve.geteMax(), "#.##");
		case 3:
			if (curve instanceof OSBCurve) {
				OSBCurve osb = (OSBCurve)curve;
				return Formatters.getInstance().format(osb.getR2(), "#.##");
			}
			break;
		case 4:
			if (curve instanceof OSBCurve) {
				OSBCurve osb = (OSBCurve)curve;
				return Formatters.getInstance().format(osb.getHill(), "#.##");
			}
			break;
		case 5:
			return CurveService.getInstance().getCurveImage(curve, imageX, imageY);
		}
		return null;
	}

	@Override
	public Object getSelectionValue(Compound rowObject, int column) {
		if (column < baseColumnCount) {
			return rowObject;
		} else {
			Feature f = features.get((column-baseColumnCount)/featureColumnCount);
			return getCurve(rowObject, f);
		}
	}

	@Override
	public int getColumnCount() {
		return baseColumnCount + (features.size()*featureColumnCount);
	}

	@Override
	public String getColumnProperty(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public int getColumnIndex(String propertyName) {
		return CollectionUtils.find(columnNames, propertyName);
	}

	@Override
	public String getTooltipText(Compound rowObject, int colIndex) {
		if (rowObject == null) return columnTooltips[colIndex];
		if (colIndex == 2) return PlateValidationStatus.getByCode(rowObject.getPlate().getValidationStatus()).toString();
		if (colIndex == 3) return CompoundValidationStatus.getByCode(rowObject.getValidationStatus()).toString();
		return null;
	}

	@Override
	public int[] getColumnWidths() {
		int[] widths = new int[columnNames.length];
		widths[0] = 110;
		widths[1] = 85;
		widths[2] = 35;
		widths[3] = 35;
		widths[4] = 65;
		widths[5] = 60;
		widths[6] = 90;
		widths[7] = 70;
		widths[8] = 60;
		widths[9] = -1;
		
		int index = baseColumnCount;
		while (index < widths.length) {
			widths[index++] = 45;
			widths[index++] = 60;
			widths[index++] = 60;
			widths[index++] = 40;
			widths[index++] = 40;
			widths[index++] = 100;
		}
		return widths;
	}

	@Override
	public Map<int[], AbstractCellPainter> getCustomCellPainters() {
		Map<int[], AbstractCellPainter> painters = new HashMap<>();
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
				Comparator<String> comp;
				for (int i = baseColumnCount; i < columnNames.length; i++) {
					int mod = (i - baseColumnCount) % featureColumnCount;
					if (mod == 0) comp = CurveComparators.CENSOR_COMPARATOR;
					else if (mod == 1 || mod == 2) comp = CurveComparators.NUMERIC_STRING_COMPARATOR;
					else continue;

					configRegistry.registerConfigAttribute(
							SortConfigAttributes.SORT_COMPARATOR
							, comp
							, DisplayMode.NORMAL
							, columnNames[i]
					);
					configRegistry.registerConfigAttribute(
							FilterRowConfigAttributes.FILTER_COMPARATOR
							, comp
							, DisplayMode.NORMAL
							, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + i
					);
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
		for (int i=0; i<features.size(); i++) {
			Feature f = features.get(i);
			boolean osb = f.getCurveSettings().get(CurveSettings.KIND).equals("OSB");
			if (!osb) {
				indices.add(baseColumnCount + (i*featureColumnCount) + 3);
				indices.add(baseColumnCount + (i*featureColumnCount) + 4);
			}
		}
		indices.add(getStructureColumn());
		return indices;
	}

	public void saveSettings(Properties properties) {
		properties.addProperty("ACTIVE_STRATEGY", concFormat);
		properties.addProperty(CURVE_WIDTH, getCurveWidth());
		properties.addProperty(CURVE_HEIGHT, getCurveHeight());
	}

	public void loadSettings(Properties properties) {
		Object o = properties.getProperty("ACTIVE_STRATEGY");
		// Support older Saved Views.
		if (o instanceof Boolean) {
			if ((boolean) o) setConcFormat(ConcentrationFormat.Molar);
			else setConcFormat(ConcentrationFormat.LogMolar);
		} else if (o instanceof ConcentrationFormat) {
			setConcFormat((ConcentrationFormat) o);
		}
		int curveWidth = properties.getProperty(CURVE_WIDTH, getCurveWidth());
		int curveHeight = properties.getProperty(CURVE_HEIGHT, getCurveHeight());
		setCurveSize(curveWidth, curveHeight);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private Curve getCurve(Compound c, Feature f) {
		CurveGrouping cg = (c instanceof CompoundWithGrouping) ? ((CompoundWithGrouping) c).getGrouping() : null;
		return CurveService.getInstance().getCurve(c, f, cg, true);
	}
	
	private MultiploCompound getMultiploCompound(Compound c) {
		if (c instanceof MultiploCompound) return (MultiploCompound) c;
		if (c instanceof CompoundWithGrouping) return getMultiploCompound(((CompoundWithGrouping) c).getDelegate());
		return null;
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

	private class CurvePreLoader extends Job {

		private NatTable table;

		public CurvePreLoader(NatTable table) {
			super("Loading Curves");
			this.table = table;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

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

							// Send the render task to another thread, so this thread can keep loading curves.
							tp.schedule(() -> {
								CurveService.getInstance().getCurveImage(curve, imageX, imageY);
							});

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

}