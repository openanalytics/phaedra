package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ClassificationGroupingStrategy extends AbstractClassificationGroupingStrategy<Plate, Well, Feature> {

	@Override
	protected void performGrouping(Feature feature, IDataProvider<Plate, Well> dataProvider, int rowCount) {
		int size = 0;
		for (Well w : dataProvider.getCurrentItems()) {
			if (dataProvider.getCurrentFilter().get(size)) {
				FeatureClass fClass = ClassificationService.getInstance().getHighestClass(w, feature);

				String lbl = UNMATCHED;
				if (fClass != null) {
					lbl = fClass.getLabel();
					getClasses().put(lbl, fClass);
				}

				getGroups().putIfAbsent(lbl, new BitSet(rowCount));
				getGroups().get(lbl).set(size, true);
			}
			size++;
		}
	}

	@Override
	public List<Feature> getClassificationFeatures(IDataProvider<Plate, Well> dataProvider) {
		Plate plate = dataProvider.getCurrentEntities().get(0);
		ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
		return ClassificationService.getInstance().findWellClassificationFeatures(pClass);
	}

}