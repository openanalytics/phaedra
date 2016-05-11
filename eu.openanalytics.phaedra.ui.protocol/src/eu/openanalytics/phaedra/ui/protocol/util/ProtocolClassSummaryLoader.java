package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.List;
import java.util.function.Consumer;

import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.AbstractSummaryLoader;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolClassSummary;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassSummaryLoader extends AbstractSummaryLoader<ProtocolClass, ProtocolClassSummary> {

	private static final ProtocolClassSummary EMPTY_SUMMARY = new ProtocolClassSummary();

	public ProtocolClassSummaryLoader(List<ProtocolClass> input, Consumer<ProtocolClass> refreshCallback) {
		super(input, refreshCallback);
	}

	@Override
	protected ProtocolClassSummary getEmptySummary() {
		return EMPTY_SUMMARY;
	}

	@Override
	protected ProtocolClassSummary createSummary(ProtocolClass pClass) {
		return ProtocolService.getInstance().getProtocolClassSummary(pClass);
	}

}
