package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagFilter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.FlagLabelProvider.FlagMapping;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
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

	private static void addGeneralColumns(List<ColumnConfiguration> configs,
			final Supplier<DataFormatter> dataFormatSupplier) {
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
		config.setLabelProvider(new WellPropertyLabelProvider(config, WellProperty.Concentration, dataFormatSupplier));
		configs.add(config);
	}
	
}
