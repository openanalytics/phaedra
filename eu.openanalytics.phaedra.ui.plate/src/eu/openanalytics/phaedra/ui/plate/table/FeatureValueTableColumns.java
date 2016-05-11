package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import com.google.common.base.Strings;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;

public class FeatureValueTableColumns {

	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", ColumnDataType.String, 190);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				FeatureValue fv = (FeatureValue) element;
				return fv.getWell().getPlate().getExperiment().getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<FeatureValue>() {
			@Override
			public int compare(FeatureValue o1, FeatureValue o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getWell().getPlate().getExperiment().getProtocol().getName().toLowerCase().compareTo(o2.getWell().getPlate().getExperiment().getProtocol().getName().toLowerCase());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);

		config = ColumnConfigFactory.create("Experiment", ColumnDataType.String, 200);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				FeatureValue fv = (FeatureValue) element;
				return fv.getWell().getPlate().getExperiment().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<FeatureValue>() {
			@Override
			public int compare(FeatureValue o1, FeatureValue o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getWell().getPlate().getExperiment().getName().toLowerCase().compareTo(o2.getWell().getPlate().getExperiment().getName().toLowerCase());
			}
		});
		config.setTooltip("Experiment");
		configs.add(config);

		config = ColumnConfigFactory.create("Plate", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				FeatureValue fv = (FeatureValue) element;
				return fv.getWell().getPlate().getBarcode();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<FeatureValue>() {
			@Override
			public int compare(FeatureValue o1, FeatureValue o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getWell().getPlate().getBarcode().toLowerCase().compareTo(o2.getWell().getPlate().getBarcode().toLowerCase());
			}
		});
		config.setTooltip("Plate");
		configs.add(config);

		config = ColumnConfigFactory.create("Well Nr", ColumnDataType.Numeric, 60);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				FeatureValue fv = (FeatureValue) cell.getElement();
				cell.setText(NumberUtils.getWellCoordinate(fv.getWell().getRow(), fv.getWell().getColumn()));
			}
		});
		config.setSorter(new Comparator<FeatureValue>() {
			@Override
			public int compare(FeatureValue fv1, FeatureValue fv2) {
				String s1 = (fv1.getWell().getRow() < 10 ? "0" : "") + fv1.getWell().getRow() + (fv1.getWell().getColumn() < 10 ? "0" : "") + fv1.getWell().getColumn();
				String s2 = (fv2.getWell().getRow() < 10 ? "0" : "") + fv2.getWell().getRow() + (fv2.getWell().getColumn() < 10 ? "0" : "") + fv2.getWell().getColumn();
				return s1.compareTo(s2);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Feature", ColumnDataType.String, 200);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				return ((FeatureValue) element).getFeature().getName();
			}
		});
		config.setSorter(new Comparator<FeatureValue>() {
			@Override
			public int compare(FeatureValue o1, FeatureValue o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getFeature().getName().compareTo(o2.getFeature().getName());
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Normalized", ColumnDataType.String, 150);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				FeatureValue fv = ((FeatureValue) element);
				return Formatters.getInstance().format(fv.getNormalizedValue(), fv.getFeature());
			}
		});
		config.setSorter(PlateUtils.NORMALIZED_FEATURE_VALUE_SORTER);
		configs.add(config);

		config = ColumnConfigFactory.create("Raw", ColumnDataType.String, 150);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				FeatureValue fv = ((FeatureValue) element);
				if (Strings.isNullOrEmpty(fv.getRawStringValue())) {
					return Formatters.getInstance().format(fv.getRawNumericValue(), fv.getFeature());
				}
				return fv.getRawStringValue();
			}
		});
		config.setSorter(PlateUtils.RAW_FEATURE_VALUE_SORTER);
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
