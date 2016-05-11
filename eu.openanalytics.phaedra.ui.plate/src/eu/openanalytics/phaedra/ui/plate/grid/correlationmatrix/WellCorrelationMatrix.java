package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.BaseCorrelationMatrix;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;

public class WellCorrelationMatrix extends BaseCorrelationMatrix<Feature> {

	public WellCorrelationMatrix() {
		super("hca.correlationmatrix.grid.well", Feature.class);
	}

	@Override
	protected List<Well> getWells(ISelection selection) {
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		List<Well> wells = new ArrayList<>();
		plates.forEach(p -> wells.addAll(p.getWells()));
		return wells;
	}

	@Override
	protected List<Feature> getFeatures(ProtocolClass pClass) {
		return pClass.getFeatures();
	}

	@Override
	protected Feature getFeatureByID(Long featureId) {
		return ProtocolService.getInstance().getFeature(featureId);
	}

	@Override
	protected BaseFeatureInput<Feature> createBaseFeatureInput(
			List<Feature> features, List<Well> wells, CorrelationType sortType, int order) {

		return new FeatureInput(features, wells, sortType, order);
	}

}