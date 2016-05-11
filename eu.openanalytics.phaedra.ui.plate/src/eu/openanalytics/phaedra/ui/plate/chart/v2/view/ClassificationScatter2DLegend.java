package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import static eu.openanalytics.phaedra.ui.plate.chart.v2.view.ClassificationLegendView.CLASSIFICATION;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.AbstractClassificationGroupingStrategy;

public class ClassificationScatter2DLegend<ENTITY, ITEM> extends Scatter2DLegend<ENTITY, ITEM> {

	@Override
	public String getPropertyValue(String property) {
		if (CLASSIFICATION.equals(property)) {
			IGroupingStrategy<ENTITY, ITEM> activeGroupingStrategy = getDataProvider().getActiveGroupingStrategy();
			if (activeGroupingStrategy instanceof AbstractClassificationGroupingStrategy<?, ?, ?>) {
				AbstractClassificationGroupingStrategy<?, ?, ?> strategy = (AbstractClassificationGroupingStrategy<?, ?, ?>) activeGroupingStrategy;
				return strategy.getClassificationFeature();
			}
		}
		return super.getPropertyValue(property);
	}

}
