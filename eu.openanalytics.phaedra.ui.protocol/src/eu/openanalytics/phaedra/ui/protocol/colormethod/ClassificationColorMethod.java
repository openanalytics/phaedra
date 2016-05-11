package eu.openanalytics.phaedra.ui.protocol.colormethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.ui.colormethod.lookup.LookupRule;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.calculation.ClassificationService.PatternType;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class ClassificationColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = 3474842800097124469L;
	private static final String SETTING_FEATURE_ID = "feature_id";

	private Feature classificationFeature;

	@Override
	public void configure(Map<String, String> settings) {
		if (settings == null) return;
		String featureIdString = settings.get(SETTING_FEATURE_ID);
		if (featureIdString != null) {
			long featureId = Long.parseLong(featureIdString);
			classificationFeature = Screening.getEnvironment().getEntityManager().find(Feature.class, featureId);
		}
	}

	@Override
	public void getConfiguration(Map<String, String> settings) {
		if (classificationFeature != null) settings.put(SETTING_FEATURE_ID, "" + classificationFeature.getId());
	}

	@Override
	public void initialize(IColorMethodData dataset) {
		// No initialization required.
	}

	@Override
	public RGB getColor(double v) {
		FeatureClass fClass = ClassificationService.getInstance().getHighestClass(v, classificationFeature);
		if (fClass != null) return getColorFor(fClass);
		return null;
	}

	@Override
	public RGB getColor(String v) {
		FeatureClass fClass = ClassificationService.getInstance().getHighestClass(v, classificationFeature);
		if (fClass != null) return getColorFor(fClass);
		return null;
	}
	
	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new ClassificationColorMethodDialog(shell, this);
	}

	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues) {
		return getLegend(width, height, orientation, labels, highlightValues, false);
	}

	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues, boolean isWhiteBackground) {
		List<LookupRule> rules = new ArrayList<>();
		String[] legendLabels = null;
		
		if (classificationFeature != null && classificationFeature.getFeatureClasses() != null) {
			List<FeatureClass> classes = classificationFeature.getFeatureClasses();
			if (classificationFeature.isNumeric()) {
				for (FeatureClass c: classes) {
					int value = ClassificationService.getInstance().getNumericRepresentation(c);
					rules.add(new LookupRule(getColorFor(c), "eq", value));
				}
			} else {
				legendLabels = new String[classes.size()];
				for (int i=0; i<classes.size(); i++) {
					rules.add(new LookupRule(getColorFor(classes.get(i)), "eq", i));
					legendLabels[i] = PatternType.getType(classes.get(i)).getStringRepresentation(classes.get(i));
				}
			}
		}
		Collections.sort(rules, new Comparator<LookupRule>() {
			@Override
			public int compare(LookupRule o1, LookupRule o2) {
				return (int)(o1.getValue() - o2.getValue());
			}
		});
		
		return new LegendDrawer(orientation).getLookupLegend(rules, legendLabels, highlightValues, width, height, isWhiteBackground);
	}

	public Feature getClassificationFeature() {
		return classificationFeature;
	}

	public void setClassificationFeature(Feature classificationFeature) {
		this.classificationFeature = classificationFeature;
	}

	private RGB getColorFor(FeatureClass fClass) {
		int color = fClass.getRgbColor();
		return new RGB((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff);
	}
}
