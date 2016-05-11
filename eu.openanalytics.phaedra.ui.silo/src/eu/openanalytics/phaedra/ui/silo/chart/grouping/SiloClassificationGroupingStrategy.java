package eu.openanalytics.phaedra.ui.silo.chart.grouping;

import java.awt.Color;
import java.awt.Shape;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.AWTShapeConverter;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.silo.vo.Silo;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.DefaultStyle;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;

public class SiloClassificationGroupingStrategy extends DefaultGroupingStrategy<Silo, Silo> {

	private Map<String, FeatureClass> classes;
	private String columnName;

	public SiloClassificationGroupingStrategy() {
		super();
		classes = new HashMap<String, FeatureClass>();

		setStyleProvider(new DefaultStyleProvider() {
			@Override
			public synchronized Style[] getStyles(String[] groups, ChartSettings settings) {
				int groupCount = groups.length;
				Style[] updatedStyles = new Style[groupCount];

				Iterator<String> groupIterator = getGroups().keySet().iterator();
				for (int position = 0; position < groupCount; position++) {
					FeatureClass fClass = null;
					if (groupIterator.hasNext()) {
						fClass = classes.get(groupIterator.next());
					}

					PlotShape plot = PlotShape.Ellipse;
					int red = 0;
					int green = 0;
					int blue = 0;
					if (fClass != null) {
						int c = fClass.getRgbColor();
						red = (c / 256) / 256;
						green = (c / 256) % 256;
						blue = c % 256;

						if (fClass.getSymbol() != null) {
							plot = PlotShape.valueOf(fClass.getSymbol());
						}
					}

					DefaultStyle style = null;
					Color color = new Color(red, green, blue);
					String group = groups[position];
					Style originalStyle = settings.getStyle(group);

					if (originalStyle != null) {
						if (settings.isBars()) {
							style = new BarStyle(
									color
									, ((BarStyle) originalStyle).getForm()
									, ((BarStyle) originalStyle).getPlacement()
									);
						} else {
							Shape awtShape = plot.getShape(((MarkStyle) originalStyle).getSize());
							style = AWTShapeConverter.convertShape(
									awtShape
									, color
									, ((MarkStyle) originalStyle).getSize()
									, originalStyle.getOpacity()
									);
							if (settings.isLines()) {
								((MarkStyle) style).setLine(((MarkStyle) originalStyle).getLine());
								style.setLineWidth(((MarkStyle) originalStyle).getSize());
							}
						}
						style.setHidePoints(originalStyle.getHidePoints());
					} else {
						if (settings.isBars()) {
							style = new BarStyle(
									color
									, getBarFormByString(settings.getDefaultSymbolType())
									, settings.isAdjacentBars() ? BarStyle.PLACE_ADJACENT : BarStyle.PLACE_OVER
									);
						} else {
							Shape awtShape = plot.getShape(settings.getDefaultSymbolSize());
							style = AWTShapeConverter.convertShape(
									awtShape
									, color
									, settings.getDefaultSymbolSize()
									, 0
									);
							if (settings.isLines()) {
								((MarkStyle) style).setLine(MarkStyle.DOT_TO_DOT);
								style.setLineWidth(settings.getDefaultSymbolSize());
							}
						}
						style.setHidePoints(false);
					}
					settings.putStyle(group, style);
					updatedStyles[position] = style;
				}
				return updatedStyles;
			}
		});
	}

	@Override
	public String getName() {
		return "Group by Classification";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Silo, Silo> dataProvider) {
		getGroups().clear();
		classes.clear();

		int totalRowCount = dataProvider.getTotalRowCount();
		if (columnName == null || columnName.isEmpty() || totalRowCount == 0) {
			BitSet bitset = new BitSet(totalRowCount);
			bitset.set(0, bitset.size());
			getGroups().put(DEFAULT_GROUPING_NAME, bitset);
		} else {
			int col = dataProvider.getFeatureIndex(columnName);

			Silo silo = dataProvider.getKey(0);
			ProtocolClass pClass = silo.getProtocolClass();
			String featureColumn = columnName.replaceAll(" \\(\\d+\\)", "");

			if (silo.getType() == GroupType.WELL.getType()) {
				Feature feature = ProtocolUtils.getFeatureByName(featureColumn, pClass);

				float[] columnData = dataProvider.getColumnData(col, 0);
				for (int i = 0; i < totalRowCount; i++) {
					if (dataProvider.getCurrentFilter().get(i)) {
						float value = columnData[i];

						FeatureClass fClass = ClassificationService.getInstance().getHighestClass(value, feature);

						String lbl = DEFAULT_GROUPING_NAME;
						if (fClass != null) {
							lbl = fClass.getLabel();
							classes.put(lbl, fClass);
						}

						BitSet bitSet = getGroups().get(lbl);
						if (bitSet == null) {
							bitSet = new BitSet(dataProvider.getTotalRowCount());
							getGroups().put(lbl, bitSet);
						}
						bitSet.set(i, true);
					}
				}
			} else {
				SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(featureColumn, pClass);

				float[] columnData = dataProvider.getColumnData(col, 0);
				for (int i = 0; i < totalRowCount; i++) {
					if (dataProvider.getCurrentFilter().get(i)) {
						float value = columnData[i];

						FeatureClass fClass = ClassificationService.getInstance().getHighestClass(value, feature);

						String lbl = DEFAULT_GROUPING_NAME;
						if (fClass != null) {
							lbl = fClass.getLabel();
							classes.put(lbl, fClass);
						}

						BitSet bitSet = getGroups().get(lbl);
						if (bitSet == null) {
							bitSet = new BitSet(dataProvider.getTotalRowCount());
							getGroups().put(lbl, bitSet);
						}
						bitSet.set(i, true);
					}
				}
			}
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
		}

		return subsets;
	}

	public String getClassificationFeature() {
		return columnName;
	}

	public void setClassificationFeature(String columnName) {
		this.columnName = columnName;
	}

}