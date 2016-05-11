package eu.openanalytics.phaedra.ui.plate.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagFilter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagMapping;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;
import eu.openanalytics.phaedra.ui.wellimage.table.JP2KImageLabelProvider;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellTableColumns {

	public static ColumnConfiguration[] configureColumns() {
		return configureColumns(true);
	}

	public static ColumnConfiguration[] configureColumns(boolean includePlateColumns) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		if (includePlateColumns) addPlateColumns(configs);
		addGeneralColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	public static ColumnConfiguration[] configureColumns(final PlateDataAccessor dataAccessor, TableViewer tableViewer) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addImageColumn(configs, dataAccessor, tableViewer);
		addGeneralColumns(configs);
		addFeatureColumns(configs, dataAccessor, tableViewer);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addPlateColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", ColumnDataType.String, 200);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getExperiment().getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Well>() {
			@Override
			public int compare(Well o1, Well o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getPlate().getExperiment().getProtocol().getName().toLowerCase().compareTo(o2.getPlate().getExperiment().getProtocol().getName().toLowerCase());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);

		config = ColumnConfigFactory.create("Experiment", ColumnDataType.String, 200);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getExperiment().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Well>() {
			@Override
			public int compare(Well o1, Well o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getPlate().getExperiment().getName().toLowerCase().compareTo(o2.getPlate().getExperiment().getName().toLowerCase());
			}
		});
		config.setTooltip("Experiment");
		configs.add(config);

		config = ColumnConfigFactory.create("Plate", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getBarcode();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Well>() {
			@Override
			public int compare(Well o1, Well o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getPlate().getBarcode().toLowerCase().compareTo(o2.getPlate().getBarcode().toLowerCase());
			}
		});
		config.setTooltip("Plate");
		configs.add(config);

	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Well Nr", ColumnDataType.Numeric, 60);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				Well well = (Well)cell.getElement();
				cell.setText(NumberUtils.getWellCoordinate(well.getRow(), well.getColumn()));
			}
		});
		config.setSorter(new Comparator<Well>() {

			@Override
			public int compare(Well w1, Well w2) {
				String s1 = (w1.getRow() < 10 ? "0" : "") + w1.getRow() + (w1.getColumn() < 10 ? "0" : "") + w1.getColumn();
				String s2 = (w2.getRow() < 10 ? "0" : "") + w2.getRow() + (w2.getColumn() < 10 ? "0" : "") + w2.getColumn();
				return s1.compareTo(s2);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Row", "getRow", ColumnDataType.Numeric, 60);
		configs.add(config);

		config = ColumnConfigFactory.create("Column", "getColumn", ColumnDataType.Numeric, 60);
		configs.add(config);

		FlagMapping[] mappings = new FlagLabelProvider.FlagMapping[] {
				new FlagLabelProvider.FlagMapping(FlagFilter.Negative, "flag_red.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.Positive, "flag_green.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.All, "flag_white.png")
		};

		config = ColumnConfigFactory.create("V", ColumnDataType.Numeric, 25);
		CellLabelProvider labelProvider = new FlagLabelProvider(config, "getStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Well>() {
			@Override
			public int compare(Well o1, Well o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.compare(o1.getStatus(), o2.getStatus());
			}
		});
		config.setTooltip("Well Validation Status");
		configs.add(config);

		config = ColumnConfigFactory.create("Well Type", "getWellType", ColumnDataType.String, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", ColumnDataType.String, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Compound", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well)element;
				Compound c = well.getCompound();
				if (c != null) {
					return c.getType() + " " + c.getNumber();
				}
				return "";
			}
		};
		config.setSorter(PlateUtils.WELL_COMPOUND_NR_SORTER);
		config.setLabelProvider(labelProvider);
		config.setTooltip("Compound");
		configs.add(config);

		config = ColumnConfigFactory.create("Concentration", "getCompoundConcentration", ColumnDataType.Numeric, 90);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				Well well = (Well)cell.getElement();
				double conc = well.getCompoundConcentration();
				cell.setText(String.valueOf(conc));
			}
		});
		configs.add(config);
	}

	private static void addImageColumn(List<ColumnConfiguration> configs, PlateDataAccessor dataAccessor, TableViewer tableViewer) {
		if (dataAccessor == null || !dataAccessor.getPlate().isImageAvailable()) return;

		Plate plate = dataAccessor.getPlate();
		float aspectRatio = ImageRenderService.getInstance().getWellImageAspectRatio(plate);
		ColumnConfiguration config = ColumnConfigFactory.create("Well Image", ColumnDataType.JP2K_IMAGE, 100, aspectRatio );
		config.setLabelProvider(new WellImageLabelProvider(config, tableViewer, PlateUtils.getProtocolClass(plate)));
		configs.add(config);
	}

	private static void addFeatureColumns(List<ColumnConfiguration> configs, final PlateDataAccessor dataAccessor, TableViewer tableViewer) {
		List<Feature> features = dataAccessor.getPlate().getExperiment().getProtocol().getProtocolClass().getFeatures();

		for (final Feature f: features) {
			ColumnConfiguration config = ColumnConfigFactory.create(f.getDisplayName(), ColumnDataType.Numeric, 90);
			RichLabelProvider labelProvider = new WellFeatureLabelProvider(config, f, dataAccessor, tableViewer);
			config.setLabelProvider(labelProvider);
			config.setSorter(new Comparator<Well>() {
				@Override
				public int compare(Well w1, Well w2) {
					if (f.isNumeric()) {
						double v1 = dataAccessor.getNumericValue(w1, f, f.getNormalization());
						double v2 = dataAccessor.getNumericValue(w2, f, f.getNormalization());
						if (Double.isNaN(v1) && Double.isNaN(v2)) return 0;
						if (Double.isNaN(v1)) return -1;
						if (Double.isNaN(v2)) return 1;
						if (v1 > v2) return 1;
						if (v1 == v2) return 0;
						return -1;
					} else {
						String s1 = dataAccessor.getStringValue(w1,f);
						String s2 = dataAccessor.getStringValue(w2,f);
						if (s1 == null && s2 == null) return 0;
						if (s1 == null) return -1;
						if (s2 == null) return 1;
						return s1.compareTo(s2);
					}
				}
			});
			config.setTooltip(f.getDisplayName());
			configs.add(config);
		}
	}

	private static class WellImageLabelProvider extends JP2KImageLabelProvider {

		private TableViewer tableViewer;
		private ProtocolClass pClass;

		public WellImageLabelProvider(ColumnConfiguration config, TableViewer tableViewer, ProtocolClass pClass) {

			super(config, false);

			this.pClass = pClass;
			this.tableViewer = tableViewer;
		}

		@Override
		public ImageData getImageData(Object element, Object settings) {
			Well well = (Well)element;
			try {
				return ImageRenderService.getInstance().getWellImageData(well
						, getCurrentWidth(), getCurrentWidth(), (boolean[]) settings);
			} catch (IOException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
			return null;
		}

		@Override
		public boolean[] getDefaultChannels() {
			ProtocolClass pClass = getProtocolClass();
			List<ImageChannel> imageChannels = pClass.getImageSettings().getImageChannels();
			boolean[] defaultChannels = new boolean[imageChannels.size()];
			for (int i = 0; i < defaultChannels.length; i++) {
				defaultChannels[i] = imageChannels.get(i).isShowInPlateView();
			}
			return defaultChannels;
		}

		@Override
		protected void settingsChanged(Object settings) {
			tableViewer.getTable().redraw();
		}

		@Override
		protected ProtocolClass getProtocolClass() {
			return pClass;
		}
	}

	private static class WellFeatureLabelProvider extends RichLabelProvider {

		private Feature feature;
		private PlateDataAccessor dataAccessor;

		private IColorMethod colorMethod;
		private ColorStore colorStore;
		private boolean useColors = true;
		private IPropertyChangeListener prefListener;

		public WellFeatureLabelProvider(ColumnConfiguration config,
				Feature feature, PlateDataAccessor dataAccessor, final TableViewer tableViewer) {
			super(config);

			this.feature = feature;
			this.dataAccessor = dataAccessor;
			useColors = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.WELL_TABLE_COLORS);

			colorMethod = ColorMethodFactory.createColorMethod(feature);
			colorMethod.initialize(ColorMethodFactory.createData(dataAccessor, feature, feature.getNormalization()));
			colorStore = new ColorStore();

			prefListener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(Prefs.WELL_TABLE_COLORS)) {
						useColors = (Boolean)event.getNewValue();
						tableViewer.refresh();
					}
				}
			};
			Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		}

		@Override
		public String getText(Object element) {
			Well well = (Well)element;
			if (feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				return Formatters.getInstance().format(value, feature);
			} else {
				return dataAccessor.getStringValue(well, feature);
			}
		}

		@Override
		public Color getForeground(Object element) {
			Well well = (Well)element;
			if (useColors && feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = colorMethod.getColor(value);
				return ColorUtils.getTextColor(rgb);
			}

			return null;
		}

		@Override
		public Color getBackground(Object element) {
			Well well = (Well)element;
			if (useColors && feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = colorMethod.getColor(value);
				if (rgb != null) return colorStore.get(rgb);
			}

			return null;
		}

		@Override
		public void dispose() {
			Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
			colorStore.dispose();
			super.dispose();
		}
	}
}
