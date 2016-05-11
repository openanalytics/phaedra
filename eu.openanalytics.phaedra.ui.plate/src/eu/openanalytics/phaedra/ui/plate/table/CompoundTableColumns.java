package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public class CompoundTableColumns {

	private static Image statusOkImg = IconManager.getIconImage("flag_green.png");
	private static Image statusNOkImg = IconManager.getIconImage("flag_red.png");
	private static Image statusNotSetImg = IconManager.getIconImage("flag_white.png");
	private static Image statusNotNeededImg = IconManager.getIconImage("flag_blue.png");

	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", ColumnDataType.String, 200);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Compound c = (Compound) element;
				return c.getPlate().getExperiment().getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Compound>() {
			@Override
			public int compare(Compound c1, Compound c2) {
				if (c1 == null) return -1;
				if (c2 == null) return 1;
				return c1.getPlate().getExperiment().getProtocol().getName().compareTo(c2.getPlate().getExperiment().getProtocol().getName());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);


		config = ColumnConfigFactory.create("Experiment", ColumnDataType.String, 200);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Compound c = (Compound) element;
				return c.getPlate().getExperiment().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Compound>() {
			@Override
			public int compare(Compound c1, Compound c2) {
				if (c1 == null) return -1;
				if (c2 == null) return 1;
				return c1.getPlate().getExperiment().getName().compareTo(c2.getPlate().getExperiment().getName());
			}
		});
		config.setTooltip("Experiment");
		configs.add(config);

		config = ColumnConfigFactory.create("Plate", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				Compound c = (Compound) element;
				return c.getPlate().getBarcode();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Compound>() {
			@Override
			public int compare(Compound c1, Compound c2) {
				if (c1 == null) return -1;
				if (c2 == null) return 1;
				return c1.getPlate().getBarcode().compareTo(c2.getPlate().getBarcode());
			}
		});
		config.setTooltip("Plate");
		configs.add(config);

		config = ColumnConfigFactory.create("V", ColumnDataType.String, 30);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public Image getImage(Object element) {
				return getIconForStatus(((Compound) element).getValidationStatus());
			}
			@Override
			public String getText(Object element) {
				return "";
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Compound>(){
			@Override
			public int compare(Compound c1, Compound c2) {
				if (c1 == null) return -1;
				if (c2 == null) return 1;
				return Integer.compare(c1.getValidationStatus(), c2.getValidationStatus());
			}
		});
		config.setTooltip("Validation Status");
		configs.add(config);

		config = ColumnConfigFactory.create("Compound Type", "getType", ColumnDataType.String, 120);
		configs.add(config);

		config = ColumnConfigFactory.create("Compound Nr", "getNumber", ColumnDataType.String, 120);
		configs.add(config);

		config = ColumnConfigFactory.create("Saltform", "getSaltform", ColumnDataType.String, 120);
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static Image getIconForStatus(int status) {
		if (status > 1) return statusOkImg;
		else if (status == 1) return statusNotNeededImg;
		else if (status < 0) return statusNOkImg;
		else return statusNotSetImg;
	}
}
