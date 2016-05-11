package eu.openanalytics.phaedra.ui.protocol.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolClassSummaryLoader;

public class ProtocolClassTableColumns {

	public static ColumnConfiguration[] configureColumns(ProtocolClassSummaryLoader summaryLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addProtocolColumns(configs, summaryLoader);
		addFeatureColumns(configs, summaryLoader);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol Class Id", "getId", ColumnDataType.Numeric, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Protocol Class Name", "getName", ColumnDataType.String, 250);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", ColumnDataType.String, 250);
		configs.add(config);
	}

	private static void addProtocolColumns(List<ColumnConfiguration> configs, ProtocolClassSummaryLoader summaryLoader) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocols", ColumnDataType.String, 100);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				ProtocolClass pc = (ProtocolClass)element;
				return "" + summaryLoader.getSummary(pc).protocols;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<ProtocolClass>(){
			@Override
			public int compare(ProtocolClass p1, ProtocolClass p2) {
				return Integer.compare(summaryLoader.getSummary(p1).protocols, summaryLoader.getSummary(p2).protocols);
			}
		});
		config.setTooltip("Protocols");
		configs.add(config);
	}

	private static void addFeatureColumns(List<ColumnConfiguration> configs, ProtocolClassSummaryLoader summaryLoader) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Well Features", ColumnDataType.String, 100);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				ProtocolClass pc = (ProtocolClass)element;
				return "" + summaryLoader.getSummary(pc).features;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<ProtocolClass>(){
			@Override
			public int compare(ProtocolClass p1, ProtocolClass p2) {
				return Integer.compare(summaryLoader.getSummary(p1).features, summaryLoader.getSummary(p2).features);
			}
		});
		config.setTooltip("Well Features");
		configs.add(config);

		config = ColumnConfigFactory.create("Subwell Features", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				ProtocolClass pc = (ProtocolClass)element;
				return "" + summaryLoader.getSummary(pc).swFeatures;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<ProtocolClass>(){
			@Override
			public int compare(ProtocolClass p1, ProtocolClass p2) {
				return Integer.compare(summaryLoader.getSummary(p1).swFeatures, summaryLoader.getSummary(p2).swFeatures);
			}
		});
		config.setTooltip("Subwell Features");
		configs.add(config);

		config = ColumnConfigFactory.create("Image Channels", ColumnDataType.String, 100);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				ProtocolClass pc = (ProtocolClass)element;
				return "" + summaryLoader.getSummary(pc).imageChannels;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<ProtocolClass>(){
			@Override
			public int compare(ProtocolClass p1, ProtocolClass p2) {
				return Integer.compare(summaryLoader.getSummary(p1).imageChannels, summaryLoader.getSummary(p2).imageChannels);
			}
		});
		config.setTooltip("Image Channels");
		configs.add(config);
	}

}
