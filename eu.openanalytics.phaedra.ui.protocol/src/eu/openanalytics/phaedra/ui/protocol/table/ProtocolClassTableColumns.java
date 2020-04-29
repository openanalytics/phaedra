package eu.openanalytics.phaedra.ui.protocol.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectComparator;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectLabelProvider;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolClassSummary;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassTableColumns {
	
	public static ColumnConfiguration[] configureColumns(AsyncDataLoader<ProtocolClass> dataLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		final AsyncDataLoader<ProtocolClass>.DataAccessor<ProtocolClassSummary> summaryAccessor = dataLoader.addDataRequest(
				(protocolClass) -> ProtocolService.getInstance().getProtocolClassSummary(protocolClass) );
		addProtocolColumns(configs, summaryAccessor);
		addFeatureColumns(configs, summaryAccessor);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol Class Id", "getId", DataType.Integer, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Protocol Class Name", "getName", DataType.String, 250);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 250);
		configs.add(config);
	}

	private static void addProtocolColumns(List<ColumnConfiguration> configs,
			final AsyncDataLoader<ProtocolClass>.DataAccessor<ProtocolClassSummary> summaryAccessor) {
		ColumnConfiguration config;
		ToIntFunction<ProtocolClassSummary> summaryIntF;

		config = ColumnConfigFactory.create("Protocols", DataType.Integer, 100);
		config.setTooltip("Protocols");
		summaryIntF = (protocolClass) -> protocolClass.protocols;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);
	}

	private static void addFeatureColumns(List<ColumnConfiguration> configs,
			final AsyncDataLoader<ProtocolClass>.DataAccessor<ProtocolClassSummary> summaryAccessor) {
		ColumnConfiguration config;
		ToIntFunction<ProtocolClassSummary> summaryIntF;

		config = ColumnConfigFactory.create("Well Features", DataType.Integer, 100);
		config.setTooltip("Well Features");
		summaryIntF = (protocolClass) -> protocolClass.features;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("Subwell Features", DataType.Integer, 100);
		config.setTooltip("Subwell Features");
		summaryIntF = (protocolClass) -> protocolClass.swFeatures;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);

		config = ColumnConfigFactory.create("Image Channels", DataType.Integer, 100);
		config.setTooltip("Image Channels");
		summaryIntF = (protocolClass) -> protocolClass.imageChannels;
		config.setLabelProvider(new SummaryIntLabelProvider(summaryAccessor, summaryIntF));
		config.setSortComparator(createIntComparator(summaryAccessor, summaryIntF));
		configs.add(config);
	}
	
	
	public static final Comparator<ProtocolClass> DEFAULT_COMPARATOR = ProtocolUtils.PROTOCOLCLASS_NAME_SORTER;
	
	private static <D> Comparator<ProtocolClass> createIntComparator(final AsyncDataLoader<ProtocolClass>.DataAccessor<D> dataAccessor,
			final ToIntFunction<D> f) {
		return AsyncDataDirectComparator.comparingInt(dataAccessor, f)
				.thenComparing(DEFAULT_COMPARATOR);
	}
	
	private static class SummaryIntLabelProvider extends AsyncDataDirectLabelProvider<ProtocolClass, ProtocolClassSummary> {
		
		private final ToIntFunction<ProtocolClassSummary> function;
		
		public SummaryIntLabelProvider(final AsyncDataLoader<ProtocolClass>.DataAccessor<ProtocolClassSummary> summaryAccessor,
				final ToIntFunction<ProtocolClassSummary> function) {
			super(summaryAccessor);
			this.function = function;
		}
		
		@Override
		protected String getText(final ProtocolClass element, final ProtocolClassSummary data) {
			return Integer.toString(this.function.applyAsInt(data));
		}
		
	}
	
}
