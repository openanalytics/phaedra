package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.theme.PhaedraThemes;
import eu.openanalytics.phaedra.base.ui.util.viewer.BasicNumericValueLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.ConditionalLabelProvider;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentSummaryLoader;

public class ExperimentTableColumns {
	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	public static ColumnConfiguration[] configureColumns(ExperimentSummaryLoader summaryLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addSummaryColumns(configs, summaryLoader);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("", DataType.String, 30);
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

	private static  void addSummaryColumns(List<ColumnConfiguration> configs, ExperimentSummaryLoader summaryLoader) {
		ColumnConfiguration config;
		Color progressColor = PhaedraThemes.GREEN_BACKGROUND_INDICATOR_COLOR.getColor();

		config = ColumnConfigFactory.create("#P", DataType.Integer, 50);
		config.setLabelProvider(new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).plates;
			}
		});
		config.setTooltip("Number of Plates");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).plates, summaryLoader.getSummary(o2).plates);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PC", DataType.Real, 50);
		config.setLabelProvider(ConditionalLabelProvider.withProgressBar(
				new BasicNumericValueLabelProvider() {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return ""+ summaryLoader.getSummary(exp).platesToCalculate;
			}
			@Override
			public double getNumericValue(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToCalculate;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		}, progressColor ));
		config.setTooltip("Number of Plates to calculate");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToCalculate, summaryLoader.getSummary(o2).platesToCalculate);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PV", DataType.Real, 50);
		config.setLabelProvider(ConditionalLabelProvider.withProgressBar(
				new BasicNumericValueLabelProvider() {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToValidate;
			}
			@Override
			public double getNumericValue(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToValidate;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		}, progressColor ));
		config.setTooltip("Number of Plates to validate");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToValidate, summaryLoader.getSummary(o2).platesToValidate);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PA", DataType.Real, 50);
		config.setLabelProvider(ConditionalLabelProvider.withProgressBar(
				new BasicNumericValueLabelProvider() {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToApprove;
			}
			@Override
			public double getNumericValue(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToApprove;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		}, progressColor ));
		config.setTooltip("Number of Plates to approve");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToApprove, summaryLoader.getSummary(o2).platesToApprove);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PE", DataType.Integer, 50);
		config.setLabelProvider(ConditionalLabelProvider.withProgressBar(
				new BasicNumericValueLabelProvider() {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToExport;
			}
			@Override
			public double getNumericValue(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToExport;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		}, progressColor ));
		config.setTooltip("Number of Plates to export");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToExport, summaryLoader.getSummary(o2).platesToExport);
			}
		});
		configs.add(config);

		// Compound counts -----------------------

		config = ColumnConfigFactory.create("#DRC", DataType.Integer, 50);
		config.setLabelProvider(new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).crcCount;
			}
		});
		config.setTooltip("Number of Compounds with Dose-Response Curves");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).crcCount, summaryLoader.getSummary(o2).crcCount);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#SDP", DataType.Integer, 50);
		config.setLabelProvider(new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).screenCount;
			}
		});
		config.setTooltip("Number of Single-Dose Points");
		config.setSortComparator(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).screenCount, summaryLoader.getSummary(o2).screenCount);
			}
		});
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
}
