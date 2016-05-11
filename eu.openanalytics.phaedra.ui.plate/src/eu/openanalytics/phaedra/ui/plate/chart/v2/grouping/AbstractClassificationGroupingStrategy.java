package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import java.awt.Color;
import java.awt.Shape;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.AWTShapeConverter;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.DefaultStyle;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;

public abstract class AbstractClassificationGroupingStrategy<ENTITY, ITEM, FEATURE extends IFeature>
extends DefaultGroupingStrategy<ENTITY, ITEM> {

	protected static final String UNMATCHED = "Unmatched";

	private Map<String, FeatureClass> classes;
	private FEATURE classificationFeature;

	public AbstractClassificationGroupingStrategy() {
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
		return "Group by classification";
	}

	@Override
	public final BitsRowSubset[] groupData(IDataProvider<ENTITY, ITEM> dataProvider) {
		getGroups().clear();
		classes.clear();

		int rowCount = dataProvider.getTotalRowCount();
		if (rowCount == 0) {
			return new BitsRowSubset[] { new BitsRowSubset(UNMATCHED, new BitSet()) };
		}

		if (classificationFeature == null) {
			List<FEATURE> features = getClassificationFeatures(dataProvider);
			if (!features.isEmpty()) classificationFeature = features.get(0);
		}

		if (classificationFeature == null) {
			// No classification feature found: put everything in one group.
			BitSet bitSet = new BitSet();
			bitSet.set(0, rowCount, true);
			getGroups().put(UNMATCHED, bitSet);
		} else {
			performGrouping(classificationFeature, dataProvider, rowCount);
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), entry.getValue());
		}

		return subsets;
	}

	protected abstract void performGrouping(FEATURE feature, IDataProvider<ENTITY, ITEM> dataProvider, int rowCount);

	protected abstract List<FEATURE> getClassificationFeatures(IDataProvider<ENTITY, ITEM> dataProvider);

	protected Map<String, FeatureClass> getClasses() {
		return classes;
	}

	public String[] getClassificationLabels(IDataProvider<ENTITY, ITEM> dataProvider) {
		List<FEATURE> classificationFeatures = getClassificationFeatures(dataProvider);
		return CollectionUtils.transformToStringArray(classificationFeatures, ProtocolUtils.FEATURE_NAMES);
	}

	public String getClassificationFeature() {
		if (classificationFeature == null) return "";
		return classificationFeature.getName();
	}

	public void setClassificationFeature(FEATURE classificationFeature) {
		this.classificationFeature = classificationFeature;
	}

	public void setClassificationFeature(String classificationLabel, IDataProvider<ENTITY, ITEM> dataProvider) {
		List<FEATURE> features = getClassificationFeatures(dataProvider);
		for (FEATURE f : features) {
			if (f.getName().equals(classificationLabel)) {
				classificationFeature = f;
				return;
			}
		}
	}

}