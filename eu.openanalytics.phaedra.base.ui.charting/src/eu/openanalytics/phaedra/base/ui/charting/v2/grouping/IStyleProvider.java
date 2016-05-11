package eu.openanalytics.phaedra.base.ui.charting.v2.grouping;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import uk.ac.starlink.ttools.plot.Style;

public interface IStyleProvider {

	Style[] getStyles(String[] groups, ChartSettings settings);

}
