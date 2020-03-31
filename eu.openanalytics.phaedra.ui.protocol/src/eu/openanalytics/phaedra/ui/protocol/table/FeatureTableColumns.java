package eu.openanalytics.phaedra.ui.protocol.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.util.FeaturePropertyProvider;

public class FeatureTableColumns {
	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;
	
		config = ColumnConfigFactory.create("Feature Id", "getId", DataType.Integer, 80);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Feature name", "getName", DataType.String, 250);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Protocol Class", DataType.String, 150);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return f.getProtocolClass().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return f1.getProtocolClass().getName().compareTo(f2.getProtocolClass().getName());
			}
		});
		config.setTooltip("Protocol Class");
		configs.add(config);
		
		config = ColumnConfigFactory.create("Key", DataType.Boolean, 35);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return FeaturePropertyProvider.getValue("Key", f);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return Boolean.compare(f1.isKey(), f2.isKey());
			}
		});
		config.setTooltip("Key");
		configs.add(config);		
		
		config = ColumnConfigFactory.create("N", DataType.Boolean, 35);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return FeaturePropertyProvider.getValue("Numeric", f);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return Boolean.compare(f1.isNumeric(), f2.isNumeric());
			}
		});
		config.setTooltip("Numeric");
		configs.add(config);

		config = ColumnConfigFactory.create("Curve", DataType.String, 85);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return FeaturePropertyProvider.getValue("Curve", f);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return FeaturePropertyProvider.getValue("Curve", f1).compareTo(FeaturePropertyProvider.getValue("Curve", f2));
			}
		});
		config.setTooltip("Curve");
		configs.add(config);
		
		config = ColumnConfigFactory.create("Low Control", DataType.Boolean, 85);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return FeaturePropertyProvider.getValue("Low Control", f);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return FeaturePropertyProvider.getValue("Low Control", f1).compareTo(FeaturePropertyProvider.getValue("Low Control", f2));
			}
		});
		config.setTooltip("High Control");
		configs.add(config);
		
		config = ColumnConfigFactory.create("High Control", DataType.Boolean, 85);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return FeaturePropertyProvider.getValue("High Control", f);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				if (f1 == null) return -1;
				if (f2 == null) return 1;
				return FeaturePropertyProvider.getValue("High Control", f1).compareTo(FeaturePropertyProvider.getValue("High Control", f2));
			}
		});
		config.setTooltip("High Control");
		configs.add(config);
	
		config = ColumnConfigFactory.create("Alias", "getShortName", DataType.String, 80);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 200);
		configs.add(config);
	
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
