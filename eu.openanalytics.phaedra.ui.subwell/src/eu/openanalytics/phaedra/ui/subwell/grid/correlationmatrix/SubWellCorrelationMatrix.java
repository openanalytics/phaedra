package eu.openanalytics.phaedra.ui.subwell.grid.correlationmatrix;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.plate.grid.BaseCorrelationMatrix;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.BaseFeatureInput;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;

public class SubWellCorrelationMatrix extends BaseCorrelationMatrix<SubWellFeature> {

	public SubWellCorrelationMatrix() {
		super("hca.correlationmatrix.grid.subwell", SubWellFeature.class);
	}

	@Override
	protected List<Well> getWells(ISelection selection) {
		return SelectionUtils.getObjects(selection, Well.class);
	}

	@Override
	protected List<SubWellFeature> getFeatures(ProtocolClass pClass) {
		return pClass.getSubWellFeatures();
	}

	@Override
	protected SubWellFeature getFeatureByID(Long featureId) {
		return ProtocolService.getInstance().getSubWellFeature(featureId);
	}

	@Override
	protected BaseFeatureInput<SubWellFeature> createBaseFeatureInput(
			List<SubWellFeature> features, List<Well> wells, CorrelationType sortType, int order) {

		return new SubwellFeatureInput(features, wells, sortType, order);
	}

}