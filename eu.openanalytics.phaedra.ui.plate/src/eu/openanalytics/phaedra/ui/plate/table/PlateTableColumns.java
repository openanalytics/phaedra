package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagFilter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ProgressBarLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.util.PlateSummaryLoader;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class PlateTableColumns {

	public static ColumnConfiguration[] configureColumns(boolean showExperimentFields, PlateSummaryLoader summaryLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		if (showExperimentFields) addExperimentColumns(configs);
		addGeneralColumns(configs, summaryLoader);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addExperimentColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", ColumnDataType.String, 200);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Plate plate = (Plate) element;
				return plate.getExperiment().getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getExperiment().getProtocol().getName().toLowerCase().compareTo(o2.getExperiment().getProtocol().getName().toLowerCase());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);

		config = ColumnConfigFactory.create("Experiment", ColumnDataType.String, 200);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Plate plate = (Plate) element;
				return plate.getExperiment().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getExperiment().getName().toLowerCase().compareTo(o2.getExperiment().getName().toLowerCase());
			}
		});
		config.setTooltip("Experiment");
		configs.add(config);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs, PlateSummaryLoader summaryLoader) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Plate Id", "getId", ColumnDataType.Numeric, 60);
		configs.add(config);

		FlagLabelProvider.FlagMapping[] mappings = new FlagLabelProvider.FlagMapping[] { new FlagLabelProvider.FlagMapping(FlagFilter.One, "image_link.png"), };
		config = ColumnConfigFactory.create("Img", ColumnDataType.Numeric, 35);
		CellLabelProvider labelProvider = new FlagLabelProvider(config, "isImageAvailable", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Boolean.compare(o1.isImageAvailable(), o2.isImageAvailable());
			}
		});
		config.setTooltip("Image Available");
		configs.add(config);

		mappings = new FlagLabelProvider.FlagMapping[] { new FlagLabelProvider.FlagMapping(FlagFilter.One, "package_link.png"), };
		config = ColumnConfigFactory.create("SW", ColumnDataType.Numeric, 35);
		labelProvider = new FlagLabelProvider(config, "isSubWellDataAvailable", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Boolean.compare(o1.isSubWellDataAvailable(), o2.isSubWellDataAvailable());
			}
		});
		config.setTooltip("Sub-well Data Available");
		configs.add(config);

		Function<Object, Boolean> canEditColumn = (p) -> SecurityService.getInstance().check(Permissions.PLATE_EDIT, p);
		Consumer<Object> saver = (p) -> PlateService.getInstance().updatePlate((Plate)p);
		
		config = ColumnConfigFactory.create("Seq", "getSequence", ColumnDataType.Numeric, 50);
		config.setTooltip("Sequence");
		config.setEditingConfig(ColumnEditingFactory.create("getSequence", "setSequence", saver, canEditColumn));
		config.getEditingConfig().valueSetter = (o, v) -> {
			String value = (String) v;
			if (value == null || value.isEmpty()) return;
			((Plate)o).setSequence(Integer.valueOf(value));
			saver.accept(o);
		};
		configs.add(config);

		config = ColumnConfigFactory.create("Barcode", "getBarcode", ColumnDataType.String, 150);
		config.setEditingConfig(ColumnEditingFactory.create("getBarcode", "setBarcode", saver, canEditColumn));
		configs.add(config);

		mappings = new FlagLabelProvider.FlagMapping[] {
				new FlagLabelProvider.FlagMapping(FlagFilter.Negative, "flag_red.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.Zero, "flag_white.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.Positive, "flag_green.png") };

		config = ColumnConfigFactory.create("C", ColumnDataType.Numeric, 25);
		labelProvider = new FlagLabelProvider(config, "getCalculationStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.compare(o1.getCalculationStatus(), o2.getCalculationStatus());
			}
		});
		config.setTooltip("Calculation Status");
		configs.add(config);

		mappings = new FlagLabelProvider.FlagMapping[] {
				new FlagLabelProvider.FlagMapping(FlagFilter.Negative, "flag_red.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.One, "flag_blue.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.GreaterThanOne, "flag_green.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.All, "flag_white.png") };

		config = ColumnConfigFactory.create("V", ColumnDataType.Numeric, 25);
		labelProvider = new FlagLabelProvider(config, "getValidationStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.compare(o1.getValidationStatus(), o2.getValidationStatus());
			}
		});
		config.setTooltip("Validation Status");
		configs.add(config);

		config = ColumnConfigFactory.create("A", ColumnDataType.Numeric, 25);
		labelProvider = new FlagLabelProvider(config, "getApprovalStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.compare(o1.getApprovalStatus(), o2.getApprovalStatus());
			}
		});
		config.setTooltip("Approval Status");
		configs.add(config);

		config = ColumnConfigFactory.create("U", ColumnDataType.Numeric, 25);
		labelProvider = new FlagLabelProvider(config, "getUploadStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.compare(o1.getUploadStatus(), o2.getUploadStatus());
			}
		});
		config.setTooltip("Upload Status");
		configs.add(config);

		if (PlatformUI.isWorkbenchRunning() && summaryLoader != null) {
			config = ColumnConfigFactory.create("ZPrime", ColumnDataType.Numeric, 50);
			labelProvider = new ProgressBarLabelProvider(config, null, new Color(Display.getCurrent(), 170, 255, 170)) {
				@Override
				protected String getText(Object element) {
					Plate plate = (Plate) element;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return "";
					return StatUtils.format(summaryLoader.getSummary(plate).getStat("zprime", f, null, null));
				}

				@Override
				protected double getPercentage(Object element) {
					Plate plate = (Plate) element;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return 0;
					double value = summaryLoader.getSummary(plate).getStat("zprime", f, null, null);
					if (Double.isNaN(value)) value = 0;
					return value;
				}
			};
			config.setLabelProvider(labelProvider);
			config.setSorter(new Comparator<Plate>() {
				@Override
				public int compare(Plate o1, Plate o2) {
					if (o1 == null && o2 != null) return -1;
					if (o2 == null) return 1;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return 0;
					double v1 = summaryLoader.getSummary(o1).getStat("zprime", f, null, null);
					double v2 = summaryLoader.getSummary(o2).getStat("zprime", f, null, null);
					return NumberUtils.compare(v1, v2);
				}
			});
			config.setTooltip("ZPrime");
			configs.add(config);

			config = ColumnConfigFactory.create("SB", ColumnDataType.Numeric, 50);
			labelProvider = new RichLabelProvider(config) {
				@Override
				public String getText(Object element) {
					Plate plate = (Plate) element;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return "";
					return StatUtils.format(summaryLoader.getSummary(plate).getStat("sb", f, null, null));
				}
			};
			config.setLabelProvider(labelProvider);
			config.setSorter(new Comparator<Plate>() {
				@Override
				public int compare(Plate o1, Plate o2) {
					if (o1 == null && o2 != null) return -1;
					if (o2 == null) return 1;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return 0;
					double v1 = summaryLoader.getSummary(o1).getStat("sb", f, null, null);
					double v2 = summaryLoader.getSummary(o2).getStat("sb", f, null, null);
					return NumberUtils.compare(v1, v2);
				}
			});
			config.setTooltip("S/B");
			configs.add(config);

			config = ColumnConfigFactory.create("SN", ColumnDataType.Numeric, 50);
			labelProvider = new RichLabelProvider(config) {
				@Override
				public String getText(Object element) {
					Plate plate = (Plate) element;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return "";
					return StatUtils.format(summaryLoader.getSummary(plate).getStat("sn", f, null, null));
				}
			};
			config.setLabelProvider(labelProvider);
			config.setSorter(new Comparator<Plate>() {
				@Override
				public int compare(Plate o1, Plate o2) {
					if (o1 == null && o2 != null) return -1;
					if (o2 == null) return 1;
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f == null) return 0;
					double v1 = summaryLoader.getSummary(o1).getStat("sn", f, null, null);
					double v2 = summaryLoader.getSummary(o2).getStat("sn", f, null, null);
					return NumberUtils.compare(v1, v2);
				}
			});
			config.setTooltip("S/N");
			configs.add(config);
		}

		config = ColumnConfigFactory.create("Description", ColumnDataType.String, 200);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Plate plate = (Plate) element;
				String description = plate.getDescription();
				if (description == null) return "";
				return description.replace('\n', ' ');
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Plate>() {
			@Override
			public int compare(Plate o1, Plate o2) {
				if (o1 == null && o2 != null) return -1;
				if (o2 == null) return 1;
				if (o1.getDescription() == null && o2.getDescription() != null) return -1;
				if (o2.getDescription() == null) return 1;
				return o1.getDescription().compareTo(o2.getDescription());
			}
		});
		config.setTooltip("Description");
		config.setEditingConfig(ColumnEditingFactory.create("getDescription", "setDescription", saver, canEditColumn));
		configs.add(config);

		config = ColumnConfigFactory.create("Link Info", "getInfo", ColumnDataType.String, 150);
		configs.add(config);

		if (summaryLoader != null) {
			config = ColumnConfigFactory.create("#DRC", ColumnDataType.Numeric, 50);
			labelProvider = new RichLabelProvider(config){
				@Override
				public String getText(Object element) {
					Plate plate = (Plate)element;
					return "" + summaryLoader.getSummary(plate).crcCount;
				}
			};
			config.setLabelProvider(labelProvider);
			config.setTooltip("Number of Compounds with Dose-Response Curves");
			config.setSorter(new Comparator<Plate>() {
				@Override
				public int compare(Plate o1, Plate o2) {
					return Integer.compare(summaryLoader.getSummary(o1).crcCount, summaryLoader.getSummary(o2).crcCount);
				}
			});
			configs.add(config);

			config = ColumnConfigFactory.create("#SDP", ColumnDataType.Numeric, 50);
			labelProvider = new RichLabelProvider(config){
				@Override
				public String getText(Object element) {
					Plate plate = (Plate)element;
					return "" + summaryLoader.getSummary(plate).screenCount;
				}
			};
			config.setLabelProvider(labelProvider);
			config.setTooltip("Number of Single-Dose Points");
			config.setSorter(new Comparator<Plate>() {
				@Override
				public int compare(Plate o1, Plate o2) {
					return Integer.compare(summaryLoader.getSummary(o1).screenCount, summaryLoader.getSummary(o2).screenCount);
				}
			});
			configs.add(config);
		}

		config = ColumnConfigFactory.create("Calculation Date", "getCalculationDate", ColumnDataType.Date, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Calculation Error", "getCalculationError", ColumnDataType.String, 150);
		configs.add(config);

		config = ColumnConfigFactory.create("Validation Date", "getValidationDate", ColumnDataType.Date, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Validation By", "getValidationUser", ColumnDataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Approval Date", "getApprovalDate", ColumnDataType.Date, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Approval By", "getApprovalUser", ColumnDataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Upload Date", "getUploadDate", ColumnDataType.Date, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Upload By", "getUploadUser", ColumnDataType.String, 100);
		configs.add(config);
	}

}
