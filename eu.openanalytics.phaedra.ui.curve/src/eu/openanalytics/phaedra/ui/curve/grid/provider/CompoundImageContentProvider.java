package eu.openanalytics.phaedra.ui.curve.grid.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.ILinkedColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionDataColumnAccessor;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

//TODO Support multiplo: currently, only images from the first plate are shown.
public class CompoundImageContentProvider extends RichColumnAccessor<CompoundWithGrouping>
	implements ISelectionDataColumnAccessor<CompoundWithGrouping>, ILinkedColumnAccessor<CompoundWithGrouping> {

	private static final String CHANNELS = "channels";
	private static final String ROW_HEIGHT = "rowHeight";
	private static final String COLUMN_WIDTH = "columnWidth";

	private NatTable table;

	private CompoundGridInput gridInput;
	private List<String> concentrations;

	private String[] columnNames;
	private String[] columnTooltips;

	private int imageW = 128;
	private int imageH = 128;
	private boolean[] channels;

	private Map<String, List<Well>> wellsPerCompoundConc;
	private Map<String, Well> selectedWellPerCompoundConc;
	private Job loadJobInProgress;
	private Job loadAvailableImageJob;

	private int baseColumnCount;
	private int imageColumnCount;

	public CompoundImageContentProvider(CompoundGridInput gridInput) {
		this.gridInput = gridInput;

		if (!gridInput.isEmpty()) {
			ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(gridInput.getProtocol());
			this.channels = new boolean[currentSettings.getImageChannels().size()];
			for (int i=0; i<channels.length; i++) {
				channels[i] = currentSettings.getImageChannels().get(i).isShowInPlateView();
			}
		}

		wellsPerCompoundConc = new HashMap<>();
		selectedWellPerCompoundConc = new HashMap<>();
		concentrations = new ArrayList<>();
		for (CompoundWithGrouping gridCompound : gridInput.getGridCompounds()) {
			if (gridCompound.getWells() == null || gridCompound.getWells().isEmpty()) continue;
			for (Well w: gridCompound.getWells()) {
				String key = getCompConcKey(gridCompound, getDisplayConc(w.getCompoundConcentration()));
				CollectionUtils.addUnique(concentrations, getDisplayConc(w.getCompoundConcentration()));
				if (!wellsPerCompoundConc.containsKey(key)) {
					wellsPerCompoundConc.put(key, new ArrayList<Well>());
				}
				wellsPerCompoundConc.get(key).add(w);
			}
		}
		Collections.sort(concentrations, (c1, c2) -> Double.valueOf(c1).compareTo(Double.valueOf(c2)));

		baseColumnCount = 8;
		imageColumnCount = concentrations.size();

		columnNames = new String[baseColumnCount+imageColumnCount];
		columnNames[0] = "Experiment";
		columnNames[1] = "Plate";
		columnNames[2] = "PV";
		columnNames[3] = "CV";
		columnNames[4] = "Comp.Type";
		columnNames[5] = "Comp.Nr";
		columnNames[6] = "Saltform";
		columnNames[7] = "Samples";
		for (int i = baseColumnCount; i<baseColumnCount+imageColumnCount; i++) {
			columnNames[i] = concentrations.get(i-baseColumnCount) + " M";
		}

		columnTooltips = new String[columnNames.length];
		for (int i=0; i<columnNames.length; i++) {
			columnTooltips[i] = columnNames[i];
		}
		columnTooltips[2] = "Plate Validation Status";
		columnTooltips[3] = "Compound Validation Status";
	}

	public void setTable(NatTable table) {
		this.table = table;
	}

	public int getImageWidth() {
		return imageW;
	}

	public int getImageHeight() {
		return imageH;
	}

	public void setImageSize(int w, int h) {
		this.imageW = w;
		this.imageH = h;
	}

	public float getImageAspectRatio() {
		List<Well> wells = new ArrayList<>();
		for (CompoundWithGrouping gridCompound : gridInput.getGridCompounds()) {
			wells.addAll(gridCompound.getWells());
			if (wells.size() < 30) break;
		}
		return ImageRenderService.getInstance().getWellImageAspectRatio(wells);
	}

	@Override
	public Object getDataValue(CompoundWithGrouping c, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return c.getPlate().getExperiment().getName();
		case 1:
			return CompoundContentProvider.getBarcodes(c);
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
			return CompoundContentProvider.getSampleCount(c);
		}

		int imageIndex = columnIndex - baseColumnCount;
		String conc = concentrations.get(imageIndex);
		Well well = getCurrentWell(c, conc);

		if (well == null) return null;
		return getWellImageData(well);
	}

	@Override
	public Object getSelectionValue(CompoundWithGrouping rowObject, int column) {
		if (0 <= column - baseColumnCount) {
			int imageIndex = column - baseColumnCount;
			String conc = concentrations.get(imageIndex);
			Well well = getCurrentWell(rowObject, conc);
			if (well != null) return well;
		}
		return rowObject;
	}

	public Comparator<?> getComparator(int columnIndex) {
		return null;
	}

	@Override
	public int getColumnCount() {
		return baseColumnCount + imageColumnCount;
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
	public String getTooltipText(CompoundWithGrouping rowObject, int colIndex) {
		if (rowObject == null) return columnTooltips[colIndex];

		if (rowObject != null) {
			if (colIndex == 2) return CompoundValidationStatus.getByCode(rowObject.getValidationStatus()).toString();
		}
		return null;
	}

	@Override
	public int[] getColumnWidths() {
		int[] widths = new int[columnNames.length];
		widths[0] = 110;
		widths[1] = 85;
		widths[2] = 30;
		widths[3] = 30;
		widths[4] = 65;
		widths[5] = 60;
		widths[6] = 90;
		widths[7] = 60;

		int index = baseColumnCount;
		while (index < widths.length) {
			widths[index++] = imageW;
		}
		return widths;
	}

	public List<Integer> getDefaultHiddenColumns() {
		List<Integer> indices = new ArrayList<Integer>();
		return indices;
	}

	public void fillChannelDropdown(ToolItem channelDropdown) {
		DropdownToolItemFactory.clearChildren(channelDropdown);
		if (gridInput.isEmpty()) {
			return;
		}
		ProtocolClass pClass = gridInput.getProtocol().getProtocolClass();

		Listener listener = event -> {
			MenuItem selected = (MenuItem) event.widget;
			MenuItem[] items = selected.getParent().getItems();
			int index = CollectionUtils.find(items, selected);

			channels[index] = selected.getSelection();
			loadImagesJob();
		};

		List<ImageChannel> imgChannels = pClass.getImageSettings().getImageChannels();
		for (int i=0; i<imgChannels.size(); i++) {
			ImageChannel channel = imgChannels.get(i);
			MenuItem item = DropdownToolItemFactory.createChild(channelDropdown, channel.getName(), SWT.CHECK);
			item.addListener(SWT.Selection, listener);
			item.setSelection(channels[i]);
		}
	}

	public Job loadImagesJob() {
		if (loadJobInProgress != null) {
			// A job is already in progress: abort it.
			loadJobInProgress.cancel();
			while (loadJobInProgress != null) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
		}

		Job loadJob = new ImageLoader();
		loadJob.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				loadJobInProgress = null;
			}
		});
		loadJobInProgress = loadJob;
		loadJob.schedule();
		return loadJob;
	}

	/**
	 * @param The current Well
	 * @return List of wells with the same concentration and compound
	 */
	public List<Well> getPossibleWells(Well w) {
		String key = getCompConcKey(w.getCompound(), getDisplayConc(w.getCompoundConcentration()));
		if (wellsPerCompoundConc.containsKey(key)) {
			return wellsPerCompoundConc.get(key);
		}
		return new ArrayList<>();
	}

	/**
	 * Retrieve the selected Well for given Compound and Concentration.
	 * @param w The well whose Compound and Concentration will be used.
	 * @return
	 */
	public Well getCurrentWell(Well w) {
		return getCurrentWell(w.getCompound(), getDisplayConc(w.getCompoundConcentration()));
	}

	/**
	 * Retrieve the selected Well for given Compound and Concentration.
	 * @param c Compound
	 * @param conc Compound Concentration
	 * @return Selected Well or Null
	 */
	private Well getCurrentWell(Compound c, String conc) {
		String key = getCompConcKey(c, conc);
		synchronized (selectedWellPerCompoundConc) {
			return selectedWellPerCompoundConc.get(key);
		}
	}

	/**
	 * @param newWell, the well which image will be used
	 */
	public void replaceCurrentUsedWell(Well newWell) {
		String key = getCompConcKey(newWell.getCompound(), getDisplayConc(newWell.getCompoundConcentration()));
		synchronized (selectedWellPerCompoundConc) {
			selectedWellPerCompoundConc.put(key, newWell);
		}
		getWellImageData(newWell);
		table.doCommand(new VisualRefreshCommand());
	}

	public void saveSettings(Properties properties) {
		properties.addProperty(ROW_HEIGHT, getImageHeight());
		properties.addProperty(COLUMN_WIDTH, getImageWidth());
		properties.addProperty(CHANNELS, channels);
	}

	public void loadSettings(Properties properties) {
		loadSettings(properties, true);
	}
	
	public void loadSettings(Properties properties, boolean loadImages) {
		int imageHeight = properties.getProperty(ROW_HEIGHT, getImageHeight());
		int imageWidth = properties.getProperty(COLUMN_WIDTH, imageHeight);
		setImageSize(imageWidth, imageHeight);
		Object property = properties.getProperty(CHANNELS);
		if (property instanceof byte[]) {
			// Backwards compatibility.
			byte[] temp = (byte[]) property;
			for (int i = 0; i < temp.length && i < channels.length; i++) channels[i] = temp[i] > 0 ? true : false;
		}
		if (property instanceof boolean[]) {
			boolean[] temp = (boolean[]) property;
			for (int i = 0; i < temp.length && i < channels.length; i++) channels[i] = temp[i];
		}
		if (loadImages) loadImagesJob();
	}

	public Map<int[], AbstractCellPainter> getCustomPainters() {
		Map<int[], AbstractCellPainter> painters = new HashMap<>();
		painters.put(new int[] { 2 }, new FlagCellPainter("calc",
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

	@Override
	public int[] getLinkedColumns() {
		int nrOfImgColumns = getColumnCount() - baseColumnCount;
		int[] linkedColumns = new int[nrOfImgColumns];
		int columnIndex = baseColumnCount;
		for (int i = 0; i < nrOfImgColumns; i++) linkedColumns[i] = columnIndex++;
		return linkedColumns;
	}

	public void loadNextAvailableImage() {
		loadAvailableImage(1);
	}

	public void loadPreviousAvailableImage() {
		loadAvailableImage(-1);
	}

	/**
	 * Load the next or previous available image.
	 * @param index Should be -1 for previous or 1 for next
	 */
	private void loadAvailableImage(int index) {
		if (loadAvailableImageJob != null) loadAvailableImageJob.cancel();
		loadAvailableImageJob = new Job("Loading Images") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Images", wellsPerCompoundConc.size());

				for (String compConc : wellsPerCompoundConc.keySet()) {
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					List<Well> wells = wellsPerCompoundConc.get(compConc);
					if (wells != null && wells.size() > 1) {
						Well previousWell;
						synchronized (selectedWellPerCompoundConc) {
							previousWell = selectedWellPerCompoundConc.get(compConc);
						}
						int indexOf = wells.indexOf(previousWell) + index;

						// Continues scrolling
						if (indexOf < 0) indexOf = wells.size() - 1;
						if (indexOf >= wells.size()) indexOf = 0;

						Well well = wells.get(indexOf);
						monitor.subTask("Loading image for well " + NumberUtils.getWellCoordinate(well.getRow(), well.getColumn()));
						replaceCurrentUsedWell(well);
					}
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		loadAvailableImageJob.schedule();
	}

	private String getCompConcKey(Compound c, String conc) {
		return c.getPlate().getId() + "#" + c.toString() + "#" + conc;
	}

	private String getDisplayConc(double conc) {
		return Formatters.getInstance().format(conc, "0.##E0");
	}
	
	private class ImageLoader extends Job {

		public ImageLoader() {
			super("Loading Images");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<CompoundWithGrouping> compounds = gridInput.getGridCompounds();
			monitor.beginTask("Loading Images", compounds.size());

			// First, make an ordered list of plates (because we can only open 1 JP2K File at a time).
			Set<Plate> plates = new HashSet<>();
			for (CompoundWithGrouping c: compounds) {
				plates.add(c.getPlate());
			}

			plates.parallelStream().forEach(plate -> {
				if (!plate.isImageAvailable()) return;

				for (CompoundWithGrouping gridCompound : compounds) {
					if (gridCompound.getPlate() != plate) continue;
					if (monitor.isCanceled()) return;

					BitSet imagesAvailable = new BitSet();

					for (Well well: gridCompound.getWells()) {
						if (monitor.isCanceled()) return;
						if (table != null && table.isDisposed()) return;

						String conc = getDisplayConc(well.getCompoundConcentration());
						int index = concentrations.indexOf(conc);

						if (index < 0) continue;
						if (!imagesAvailable.get(index)) {
							imagesAvailable.set(index);

							monitor.subTask("Loading image for well " + NumberUtils.getWellCoordinate(well.getRow(), well.getColumn()));

							// Caches the image.
							getWellImageData(well);
							String key = getCompConcKey(gridCompound, conc);
							synchronized (selectedWellPerCompoundConc) {
								selectedWellPerCompoundConc.put(key, well);
							}
						}
					}
					Display.getDefault().asyncExec(() -> { if (table != null) { table.doCommand(new VisualRefreshCommand()); } });
					monitor.worked(1);
				}
			});

			monitor.done();
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			return Status.OK_STATUS;
		}
	}

	private ImageData getWellImageData(Well well) {
		try {
			ImageData imgData = ImageRenderService.getInstance().getWellImageData(well, imageW, imageH, channels);
			String key = getCompConcKey(well.getCompound(), getDisplayConc(well.getCompoundConcentration()));
			if (wellsPerCompoundConc.containsKey(key)) {
				if (wellsPerCompoundConc.get(key).size() > 1) {
					imgData = drawPlus(imgData);
				}
			}
			return imgData;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Draw white cross on image
	 * @param imgData
	 */
	private ImageData drawPlus(ImageData imgData) {
		if (imgData == null) return imgData;
		int w = imgData.width;

		Image tempImg = null;
		GC gc = null;
		Color white = null;
		try {
			tempImg = new Image(null, imgData);
			gc = new GC(tempImg);
			white = new Color(Display.getDefault(), 255, 255, 255);
			gc.setLineWidth(3);
			gc.setForeground(white);
			gc.drawLine(w-15, 9, w-5, 9);
			gc.drawLine(w-10, 5, w-10, 15);
			gc.dispose();
			return tempImg.getImageData();
		} finally {
			if (white != null) white.dispose();
			if (gc != null) gc.dispose();
			if (tempImg != null) tempImg.dispose();
		}
	}

}