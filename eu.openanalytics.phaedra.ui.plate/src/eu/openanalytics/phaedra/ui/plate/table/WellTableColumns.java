package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.CellLabelProvider;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagFilter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagMapping;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public class WellTableColumns {

	public static ColumnConfiguration[] configureColumns(boolean includePlateColumns,
			final Supplier<DataFormatter> dataFormatSupplier) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		if (includePlateColumns) addPlateColumns(configs);
		addGeneralColumns(configs, dataFormatSupplier);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addPlateColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", DataType.String, 200);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getExperiment().getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Well>() {
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

		config = ColumnConfigFactory.create("Experiment", DataType.String, 200);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getExperiment().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Well>() {
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

		config = ColumnConfigFactory.create("Plate", DataType.String, 100);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Well well = (Well) element;
				return well.getPlate().getBarcode();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Well>() {
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

	private static void addGeneralColumns(List<ColumnConfiguration> configs,
			final Supplier<DataFormatter> dataFormatSupplier) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create(WellProperty.Position, dataFormatSupplier, 60);
		config.setSortComparator(new Comparator<Well>() {
			@Override
			public int compare(Well w1, Well w2) {
				String s1 = (w1.getRow() < 10 ? "0" : "") + w1.getRow() + (w1.getColumn() < 10 ? "0" : "") + w1.getColumn();
				String s2 = (w2.getRow() < 10 ? "0" : "") + w2.getRow() + (w2.getColumn() < 10 ? "0" : "") + w2.getColumn();
				return s1.compareTo(s2);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Row, dataFormatSupplier, "Row", 50);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Column, dataFormatSupplier, "Column", 50);
		configs.add(config);

		FlagMapping[] mappings = new FlagLabelProvider.FlagMapping[] {
				new FlagLabelProvider.FlagMapping(FlagFilter.Negative, "flag_red.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.Positive, "flag_green.png"),
				new FlagLabelProvider.FlagMapping(FlagFilter.All, "flag_white.png")
		};

		config = ColumnConfigFactory.create("V", DataType.Integer, 25);
		CellLabelProvider labelProvider = new FlagLabelProvider(config, "getStatus", mappings);
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Well>() {
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

		config = ColumnConfigFactory.create(WellProperty.WellType, dataFormatSupplier, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 75);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Compound, dataFormatSupplier, 100);
		config.setSortComparator(PlateUtils.WELL_COMPOUND_NR_SORTER);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Concentration, dataFormatSupplier, 90);
		configs.add(config);
	}
	
}
