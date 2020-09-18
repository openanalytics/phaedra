package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

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
import eu.openanalytics.phaedra.base.ui.theme.PhaedraThemes;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataConditionalLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectComparator;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.ProgressBarRenderer;
import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateSummary;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.plate.util.PlateSummaryWithStats;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class PlateTableColumns {

	public static ColumnConfiguration[] configureColumns(AsyncDataLoader<Plate> dataLoader,
			boolean showExperimentFields, boolean withSummary) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		if (showExperimentFields) addExperimentColumns(configs);
		addGeneralColumns(configs, dataLoader, withSummary);
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

	private static void addGeneralColumns(List<ColumnConfiguration> configs,
			final AsyncDataLoader<Plate> dataLoader,
			final boolean withSummary) {
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

		final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor = (dataLoader != null) ?
				dataLoader.addDataRequest((plate) -> PlateSummaryWithStats.loadSummary(plate)) :
				null;
		
		if (withSummary) {
			config = ColumnConfigFactory.create("ZPrime", DataType.Real, 50);
			config.setTooltip("ZPrime");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "zprime", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "zprime", null));
			configs.add(config);

			// UR-015: Add Robust Z-Prime
			config = ColumnConfigFactory.create("Robust ZPrime", DataType.Real, 75);
			config.setTooltip("Robust ZPrime");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "robustzprime", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "robustzprime", null));
			configs.add(config);

			// UR-015: Add Pearson correlation coefficient and p-value
			config = ColumnConfigFactory.create("Pearson CC", DataType.Real, 75);
			config.setTooltip("Pearson CC");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "pearsoncc", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "pearsoncc", null));
			configs.add(config);
			
			config = ColumnConfigFactory.create("Pearson p-value", DataType.Real, 75);
			config.setTooltip("Pearson P-Value");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "pearsonpval", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "pearsonpval", null));
			configs.add(config);

			// UR-015: Add Spearman correlation coefficient and p-value
			config = ColumnConfigFactory.create("Spearman CC", DataType.Real, 75);
			config.setTooltip("Spearman CC");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "spearmancc", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "spearmancc", null));
			configs.add(config);
			
			config = ColumnConfigFactory.create("Spearman p-value", DataType.Real, 75);
			config.setTooltip("Spearman P-Value");
			config.setLabelProvider(createSummaryStatProgressBarLabelProvider(summaryAccessor, "spearmanpval", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "spearmanpval", null));
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

		if (withSummary) {
			ToIntFunction<PlateSummary> summaryIntF;
			
			config = ColumnConfigFactory.create("#DRC", DataType.Integer, 50);
			config.setTooltip("Number of Compounds with Dose-Response Curves");
			summaryIntF = (plateSummary) -> plateSummary.crcCount;
			config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
			config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
			configs.add(config);
			
			config = ColumnConfigFactory.create("#SDP", DataType.Integer, 50);
			config.setTooltip("Number of Single-Dose Points");
			summaryIntF = (plateSummary) -> plateSummary.screenCount;
			config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
			config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
			configs.add(config);
		}
		
		if (withSummary) {
			config = ColumnConfigFactory.create("SB", DataType.Real, 50);
			config.setTooltip("Signal/Background");
			config.setLabelProvider(new SummaryStatLabelProvider(summaryAccessor, "sb", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "sb", null));
			configs.add(config);
	
			config = ColumnConfigFactory.create("SN", DataType.Real, 50);
			config.setTooltip("Signal/Noise");
			config.setLabelProvider(new SummaryStatLabelProvider(summaryAccessor, "sn", null));
			config.setSortComparator(createSummaryStatComparator(summaryAccessor, "sn", null));
			configs.add(config);
			
			// PHA-644
			String lcLabel = ProtocolUtils.getCustomHCLCLabel(WellType.LC);
			String hcLabel = ProtocolUtils.getCustomHCLCLabel(WellType.HC);
			String[] controlTypes = { lcLabel, hcLabel };
			for (String controlType: controlTypes) {
				config = ColumnConfigFactory.create("%CV " + controlType, DataType.Real, 75);
				config.setLabelProvider(new SummaryStatLabelProvider(summaryAccessor, "cv", controlType));
				config.setSortComparator(createSummaryStatComparator(summaryAccessor, "cv", controlType));
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

	public static final Comparator<Plate> DEFAULT_COMPARATOR = PlateUtils.PLATE_EXP_NAME_PLATE_BARCODE_SORTER;
	
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
	
	
	
	private static <D> Comparator<Plate> createIntComparator(final AsyncDataLoader<Plate>.DataAccessor<D> dataAccessor,
			final ToIntFunction<D> f) {
		return AsyncDataDirectComparator.comparingInt(dataAccessor, f).thenComparing(DEFAULT_COMPARATOR);
	}
	
	private static class SummaryIntLabelProvider extends AsyncDataDirectLabelProvider<Plate, PlateSummary> {
		
		private final ToIntFunction<PlateSummary> function;
		
		public SummaryIntLabelProvider(final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor,
				final ToIntFunction<PlateSummary> function) {
			super(summaryAccessor);
			this.function = function;
		}
		
		@Override
		protected String getText(final Plate element, final PlateSummary data) {
			return Integer.toString(
					this.function.applyAsInt(data) );
		}
		
	}
	
	private static class SummaryStatComparator extends AsyncDataDirectComparator<Plate, PlateSummary> {
		
		private final String stat;
		private final String wellType;
		
		public SummaryStatComparator(final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor,
				final String stat, final String wellType) {
			super(summaryAccessor);
			this.stat = stat;
			this.wellType = wellType;
		}
		
		@Override
		protected int compareData(final Plate element1, final PlateSummary data1,
				final Plate element2, final PlateSummary data2) {
			final Feature f = ProtocolUIService.getInstance().getCurrentFeature();
			if (f == null) return 0;
			
			return Double.compare(
					data1.getStat(this.stat, f, this.wellType, null),
					data2.getStat(this.stat, f, this.wellType, null) );
		}
		
	}
	
	private static Comparator<Plate> createSummaryStatComparator(final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> dataAccessor,
			final String stat, final String wellType) {
		return new SummaryStatComparator(dataAccessor, stat, wellType)
				.thenComparing(DEFAULT_COMPARATOR);
	}
	
	private static class SummaryStatLabelProvider extends AsyncDataDirectLabelProvider<Plate, PlateSummary> {
		
		protected final String stat;
		protected final String wellType;
		
		public SummaryStatLabelProvider(final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor,
				final String stat, final String wellType) {
			super(summaryAccessor);
			this.stat = stat;
			this.wellType = wellType;
		}
		
		@Override
		protected String getText(final Plate element, final PlateSummary data) {
			final Feature f = ProtocolUIService.getInstance().getCurrentFeature();
			if (f == null) return "";
			
			return StatUtils.format(data.getStat(this.stat, f, this.wellType, null) );
		}
		
	}
	
	private static ConditionalLabelProvider createSummaryStatProgressBarLabelProvider(
			final AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor,
			final String stat, final String wellType) {
		return new AsyncDataConditionalLabelProvider<Plate, PlateSummary>(
				new SummaryStatLabelProvider(summaryAccessor, stat, wellType),
				new ProgressBarRenderer(PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR.getColor()) ) {
			@Override
			protected double getNumericValue(final Plate element, final PlateSummary data) {
				final Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f == null) return 0;
				
				return data.getStat(stat, f, wellType, null);
			}
		};
	}
	
}
