package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagFilter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagMapping;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ProgressBarLabelProvider;
import eu.openanalytics.phaedra.base.ui.theme.PhaedraThemes;
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

		config = ColumnConfigFactory.create("Protocol", DataType.String, 200);
		config.setLabelProvider(createTextLabelProvider(config, p -> p.getExperiment().getProtocol().getName()));
		config.setSortComparator(createTextSorter(p -> p.getExperiment().getProtocol().getName()));
		config.setTooltip("Protocol");
		configs.add(config);

		config = ColumnConfigFactory.create("Experiment", DataType.String, 200);
		config.setLabelProvider(createTextLabelProvider(config, p -> p.getExperiment().getName()));
		config.setSortComparator(createTextSorter(p -> p.getExperiment().getName()));
		config.setTooltip("Experiment");
		configs.add(config);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs, PlateSummaryLoader summaryLoader) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Plate Id", "getId", DataType.Integer, 60);
		configs.add(config);

		config = ColumnConfigFactory.create("Img", DataType.Integer, 35);
		config.setLabelProvider(new FlagLabelProvider(config, "isImageAvailable", map(FlagFilter.One, "image_link.png")));
		config.setSortComparator(createIntSorter(p -> p.isImageAvailable() ? 1 : 0));
		config.setTooltip("Image Available");
		configs.add(config);

		config = ColumnConfigFactory.create("SW", DataType.Integer, 35);
		config.setLabelProvider(new FlagLabelProvider(config, "isSubWellDataAvailable", map(FlagFilter.One, "package_link.png")));
		config.setSortComparator(createIntSorter(p -> p.isSubWellDataAvailable() ? 1 : 0));
		config.setTooltip("Sub-well Data Available");
		configs.add(config);

		Function<Object, Boolean> editableChecker = (p) -> SecurityService.getInstance().check(Permissions.PLATE_EDIT, p);
		Consumer<Object> saver = (p) -> PlateService.getInstance().updatePlate((Plate)p);
		
		config = ColumnConfigFactory.create("Seq", "getSequence", DataType.Integer, 50);
		config.setTooltip("Sequence");
		config.setEditingConfig(ColumnEditingFactory.create("getSequence", "setSequence", saver, editableChecker));
		config.getEditingConfig().valueSetter = (o, v) -> {
			String value = (String) v;
			if (value == null || value.isEmpty()) return;
			((Plate)o).setSequence(Integer.valueOf(value));
			saver.accept(o);
		};
		configs.add(config);

		config = ColumnConfigFactory.create("Barcode", "getBarcode", DataType.String, 150);
		config.setEditingConfig(ColumnEditingFactory.create("getBarcode", "setBarcode", saver, editableChecker));
		configs.add(config);

		config = ColumnConfigFactory.create("C", DataType.Integer, 25);
		config.setLabelProvider(new FlagLabelProvider(config, "getCalculationStatus",
				map(FlagFilter.Negative, "flag_red.png"),
				map(FlagFilter.Zero, "flag_white.png"),
				map(FlagFilter.Positive, "flag_green.png")));
		config.setSortComparator(createIntSorter(Plate::getCalculationStatus));
		config.setTooltip("Calculation Status");
		configs.add(config);

		FlagMapping[] mappings = {
				map(FlagFilter.Negative, "flag_red.png"),
				map(FlagFilter.One, "flag_blue.png"),
				map(FlagFilter.GreaterThanOne, "flag_green.png"),
				map(FlagFilter.All, "flag_white.png")
		};
		
		config = ColumnConfigFactory.create("V", DataType.Integer, 25);
		config.setLabelProvider(new FlagLabelProvider(config, "getValidationStatus", mappings));
		config.setSortComparator(createIntSorter(Plate::getValidationStatus));
		config.setTooltip("Validation Status");
		configs.add(config);

		config = ColumnConfigFactory.create("A", DataType.Integer, 25);
		config.setLabelProvider(new FlagLabelProvider(config, "getApprovalStatus", mappings));
		config.setSortComparator(createIntSorter(Plate::getApprovalStatus));
		config.setTooltip("Approval Status");
		configs.add(config);

		config = ColumnConfigFactory.create("U", DataType.Integer, 25);
		config.setLabelProvider(new FlagLabelProvider(config, "getUploadStatus", mappings));
		config.setSortComparator(createIntSorter(Plate::getUploadStatus));
		config.setTooltip("Upload Status");
		configs.add(config);

		if (PlatformUI.isWorkbenchRunning() && summaryLoader != null) {
			config = ColumnConfigFactory.create("ZPrime", DataType.Real, 50);
			config.setLabelProvider(createProgressLabelProvider(config,
					(p, f) -> StatUtils.format(summaryLoader.getSummary(p).getStat("zprime", f, null, null)),
					(p, f) -> summaryLoader.getSummary(p).getStat("zprime", f, null, null)));
			
			config.setSortComparator(createValueSorter((p, f) -> summaryLoader.getSummary(p).getStat("zprime", f, null, null)));
			config.setTooltip("ZPrime");
			configs.add(config);
		}

		config = ColumnConfigFactory.create("Description", DataType.String, 200);
		config.setLabelProvider(createTextLabelProvider(config, p -> p.getDescription() == null ? "" : p.getDescription().replace('\n', ' ')));
		config.setSortComparator(createTextSorter(p -> p.getDescription()));
		config.setTooltip("Description");
		config.setEditingConfig(ColumnEditingFactory.create("getDescription", "setDescription", saver, editableChecker));
		configs.add(config);

		config = ColumnConfigFactory.create("Link Info", "getInfo", DataType.String, 150);
		configs.add(config);

		if (summaryLoader != null) {
			config = ColumnConfigFactory.create("#DRC", DataType.Integer, 50);
			config.setLabelProvider(createTextLabelProvider(config, p -> String.valueOf(summaryLoader.getSummary(p).crcCount)));
			config.setSortComparator(createIntSorter(p -> summaryLoader.getSummary(p).crcCount));
			config.setTooltip("Number of Compounds with Dose-Response Curves");
			configs.add(config);

			config = ColumnConfigFactory.create("#SDP", DataType.Integer, 50);
			config.setLabelProvider(createTextLabelProvider(config, p -> String.valueOf(summaryLoader.getSummary(p).screenCount)));
			config.setSortComparator(createIntSorter(p -> summaryLoader.getSummary(p).screenCount));
			config.setTooltip("Number of Single-Dose Points");
			configs.add(config);
		}
		
		if (PlatformUI.isWorkbenchRunning() && summaryLoader != null) {
			config = ColumnConfigFactory.create("SB", DataType.Real, 50);
			config.setLabelProvider(createValueLabelProvider(config, (p, f) -> StatUtils.format(summaryLoader.getSummary(p).getStat("sb", f, null, null))));
			config.setSortComparator(createValueSorter((p, f) -> summaryLoader.getSummary(p).getStat("sb", f, null, null)));
			config.setTooltip("Signal/Background");
			configs.add(config);
	
			config = ColumnConfigFactory.create("SN", DataType.Real, 50);
			config.setLabelProvider(createValueLabelProvider(config, (p, f) -> StatUtils.format(summaryLoader.getSummary(p).getStat("sn", f, null, null))));
			config.setSortComparator(createValueSorter((p, f) -> summaryLoader.getSummary(p).getStat("sn", f, null, null)));
			config.setTooltip("Signal/Noise");
			configs.add(config);
			
			String[] controlTypes = { "LC", "HC" };
			for (String controlType: controlTypes) {
				config = ColumnConfigFactory.create("%CV " + controlType, DataType.Real, 75);
				config.setLabelProvider(createValueLabelProvider(config, (p, f) -> StatUtils.format(summaryLoader.getSummary(p).getStat("cv", f, controlType, null))));
				config.setSortComparator(createValueSorter((p, f) -> summaryLoader.getSummary(p).getStat("cv", f, controlType, null)));
				config.setTooltip("%CV " + controlType);
				configs.add(config);
			}
		}

		config = ColumnConfigFactory.create("Calculation Date", "getCalculationDate", DataType.DateTime, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Calculation Error", "getCalculationError", DataType.String, 150);
		configs.add(config);

		config = ColumnConfigFactory.create("Validation Date", "getValidationDate", DataType.DateTime, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Validation By", "getValidationUser", DataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Approval Date", "getApprovalDate", DataType.DateTime, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Approval By", "getApprovalUser", DataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Upload Date", "getUploadDate", DataType.DateTime, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Upload By", "getUploadUser", DataType.String, 100);
		configs.add(config);
	}

	private static FlagMapping map(FlagFilter filter, String icon) {
		return new FlagLabelProvider.FlagMapping(filter, icon);
	}
	
	private static RichLabelProvider createTextLabelProvider(ColumnConfiguration cfg, Function<Plate, String> textGetter) {
		return new RichLabelProvider(cfg) {
			@Override
			public String getText(Object element) {
				Plate plate = (Plate) element;
				return textGetter.apply(plate);
			}
		};
	}
	
	private static RichLabelProvider createValueLabelProvider(ColumnConfiguration cfg, BiFunction<Plate, Feature, String> textGetter) {
		return new RichLabelProvider(cfg) {
			@Override
			public String getText(Object element) {
				Plate plate = (Plate) element;
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f == null) return "";
				return textGetter.apply(plate, f);
			}
		};
	}

	private static ProgressBarLabelProvider createProgressLabelProvider(ColumnConfiguration cfg,
			BiFunction<Plate, Feature, String> textGetter,
			BiFunction<Plate, Feature, Double> valueGetter) {
		Color progressColor = PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR.getColor();
		return new ProgressBarLabelProvider(cfg, null, progressColor) {
			@Override
			protected String getText(Object element) {
				Plate plate = (Plate) element;
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f == null) return "";
				return textGetter.apply(plate, f);
			}

			@Override
			protected double getPercentage(Object element) {
				Plate plate = (Plate) element;
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f == null) return 0;
				double value = valueGetter.apply(plate, f);
				if (Double.isNaN(value)) value = 0;
				return value;
			}
		};
	}
	
	private static Comparator<Plate> createTextSorter(Function<Plate, String> textGetter) {
		return (Plate p1, Plate p2) -> { 
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			
			String s1 = textGetter.apply(p1);
			String s2 = textGetter.apply(p2);
			if (s1 == null && s2 == null) return 0;
			if (s1 == null) return -1;
			if (s2 == null) return 1;

			return s1.toLowerCase().compareTo(s2.toLowerCase());
		};
	}
	
	private static Comparator<Plate> createIntSorter(Function<Plate, Integer> valueGetter) {
		return (Plate p1, Plate p2) -> { 
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			
			int i1 = valueGetter.apply(p1);
			int i2 = valueGetter.apply(p2);
			return Integer.compare(i1, i2);
		};
	}
	
	private static Comparator<Plate> createValueSorter(BiFunction<Plate, Feature, Double> valueGetter) {
		return (Plate p1, Plate p2) -> { 
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			
			Feature f = ProtocolUIService.getInstance().getCurrentFeature();
			if (f == null) return 0;
			
			double v1 = valueGetter.apply(p1, f);
			double v2 = valueGetter.apply(p2, f);
			return NumberUtils.compare(v1, v2);
		};
	}
}
