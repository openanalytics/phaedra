package eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping;

import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.AbstractClassificationGroupingStrategy;

public class ClassificationGroupingStrategy extends AbstractClassificationGroupingStrategy<Well, Well, SubWellFeature> {

	@Override
	protected synchronized void performGrouping(SubWellFeature feature, IDataProvider<Well, Well> dataProvider, int rowCount) {
		int size = 0;
		for (Well w : dataProvider.getCurrentEntities()) {
			int amount = dataProvider.getDataSizes().get(w);
			for (int i = 0; i < amount; i++) {
				if (dataProvider.getCurrentFilter().get(size)) {
					FeatureClass fClass = ClassificationService.getInstance().getHighestClass(w, i, feature);

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
	}

	@Override
	protected List<SubWellFeature> getClassificationFeatures(IDataProvider<Well, Well> dataProvider) {
		Well well = dataProvider.getCurrentEntities().get(0);
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		return ClassificationService.getInstance().findSubWellClassificationFeatures(pClass);
	}

}