package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.theme.PhaedraThemes;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataConditionalLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectComparator;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider.ProgressBarRenderer;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.ExperimentSummary;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class ExperimentTableColumns {
	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	public static ColumnConfiguration[] configureColumns(AsyncDataLoader<Experiment> dataLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addSummaryColumns(configs, dataLoader);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("", DataType.Image, 30);
		config.setTooltip("Icon");
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setImage(IconManager.getIconImage("map.png"));
				cell.setText("");
			}
		});
		configs.add(config);

		Function<Object, Boolean> editableChecker = (e) -> SecurityService.getInstance().check(Permissions.EXPERIMENT_EDIT, e);
		Consumer<Object> saver = (e) -> PlateService.getInstance().updateExperiment((Experiment)e);
		
		config = ColumnConfigFactory.create("Experiment Id", "getId", DataType.Integer, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Name", "getName", DataType.String, 250);
		config.setEditingConfig(ColumnEditingFactory.create("getName", "setName", saver, editableChecker));
		configs.add(config);

		config = ColumnConfigFactory.create("Created On", "getCreateDate", DataType.DateTime, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Creator", "getCreator", DataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 200);
		config.setEditingConfig(ColumnEditingFactory.create("getDescription", "setDescription", saver, editableChecker));
		configs.add(config);
	}
	
	private static void addSummaryColumns(List<ColumnConfiguration> configs,
			final AsyncDataLoader<Experiment> dataLoader) {
		final AsyncDataLoader<Experiment>.DataAccessor<ExperimentSummary> summaryAccessor = dataLoader.addDataRequest(
				(experiment) -> PlateService.getInstance().getExperimentSummary(experiment) );
		
		ColumnConfiguration config;
		ToIntFunction<ExperimentSummary> summaryIntF;

		config = ColumnConfigFactory.create("#P", DataType.Integer, 50);
		config.setTooltip("Number of Plates");
		summaryIntF = (summary) -> summary.plates;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("#PC", DataType.Integer, 50);
		config.setTooltip("Number of Plates to calculate");
		summaryIntF = (summary) -> summary.platesToCalculate;
		config.setLabelProvider(createSummaryIntTodoProgressBarLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("#PV", DataType.Integer, 50);
		config.setTooltip("Number of Plates to validate");
		summaryIntF = (summary) -> summary.platesToValidate;
		config.setLabelProvider(createSummaryIntTodoProgressBarLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("#PA", DataType.Integer, 50);
		config.setTooltip("Number of Plates to approve");
		summaryIntF = (summary) -> summary.platesToApprove;
		config.setLabelProvider(createSummaryIntTodoProgressBarLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("#PE", DataType.Integer, 50);
		config.setTooltip("Number of Plates to export");
		summaryIntF = (summary) -> summary.platesToExport;
		config.setLabelProvider(createSummaryIntTodoProgressBarLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		// Compound counts -----------------------

		config = ColumnConfigFactory.create("#DRC", DataType.Integer, 50);
		config.setTooltip("Number of Compounds with Dose-Response Curves");
		summaryIntF = (summary) -> summary.crcCount;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("#SDP", DataType.Integer, 50);
		config.setTooltip("Number of Single-Dose Points");
		summaryIntF = (summary) -> summary.screenCount;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);
	}

	private static void addProtocolColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", DataType.String, 200);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return exp.getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getProtocol().getName().toLowerCase().compareTo(o2.getProtocol().getName().toLowerCase());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);
		
		//TODO Archive information
	}
	
	
	public static final Comparator<Experiment> DEFAULT_COMPARATOR = PlateUtils.EXPERIMENT_NAME_SORTER;
	
	private static <D> Comparator<Experiment> createIntComparator(final AsyncDataLoader<Experiment>.DataAccessor<D> dataAccessor,
			final ToIntFunction<D> f) {
		return AsyncDataDirectComparator.comparingInt(dataAccessor, f)
				.thenComparing(DEFAULT_COMPARATOR);
	}
	
	private static class SummaryIntLabelProvider extends AsyncDataDirectLabelProvider<Experiment, ExperimentSummary> {
		
		protected final ToIntFunction<ExperimentSummary> function;
		
		public SummaryIntLabelProvider(final AsyncDataLoader<Experiment>.DataAccessor<ExperimentSummary> summaryAccessor,
				final ToIntFunction<ExperimentSummary> function) {
			super(summaryAccessor);
			this.function = function;
		}
		
		@Override
		protected String getText(final Experiment element, final ExperimentSummary data) {
			return Integer.toString(this.function.applyAsInt(data));
		}
		
	}
	
	private static ConditionalLabelProvider createSummaryIntTodoProgressBarLabelProvider(
			final AsyncDataLoader<Experiment>.DataAccessor<ExperimentSummary> summaryAccessor,
			final ToIntFunction<ExperimentSummary> function) {
		return new AsyncDataConditionalLabelProvider<Experiment, ExperimentSummary>(
				new SummaryIntLabelProvider(summaryAccessor, function),
				new ProgressBarRenderer(PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR.getColor()) ) {
			@Override
			protected double getNumericValue(final Experiment element, final ExperimentSummary data) {
				final int total = data.plates;
				return (double)(total - function.applyAsInt(data))/total;
			}
		};
	}
	
}
