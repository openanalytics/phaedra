package eu.openanalytics.phaedra.ui.plate.chart.v2.filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.AbstractFilter;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ClassificationFilter extends AbstractFilter<Plate, Well> {

	private static final String FEATURE = "FEATURE";
	private static final String SHOW_NOT_CLASSIFIED = "SHOW_NOT_CLASSIFIED";
	private static final String FEATURE_CLASS_NAMES = "FEATURE_CLASS_NAMES";

	private ProtocolClass pClass;
	private List<Feature> features;
	private Feature feature;
	private List<FeatureClass> fClasses;
	private boolean showNotClassified;

	public ClassificationFilter(IDataProvider<Plate, Well> dataProvider) {
		super("Classification", dataProvider);

		this.features = new ArrayList<>();
		this.fClasses = new ArrayList<>();
		this.showNotClassified = true;
	}
	@Override
	public void doInitialize(final Menu parent) {
		// Filter item
		MenuItem groupFilterItem = new MenuItem(parent, SWT.PUSH);
		groupFilterItem.setText(getGroup() + (isActive() ? " [Enabled]" : ""));
		groupFilterItem.addListener(SWT.Selection, e -> {
			doApplyFilterItem(null);
			groupFilterItem.setText(getGroup() + (isActive() ? " [Enabled]" : ""));
		});
	}

	@Override
	public void doApplyFilterItem(String filterItem) {
		checkProtocol();
		ClassificationFilterDialog<Feature> dialog = new ClassificationFilterDialog<>(
				Display.getDefault().getActiveShell()
				, features, feature, fClasses, showNotClassified);

		if (dialog.open() == Window.OK) {
			feature = dialog.getFeature();
			fClasses = dialog.getFeatureClasses();
			showNotClassified = dialog.isShowNotClassified();

			super.doApplyFilterItem(filterItem);
		}
	}

	@Override
	public void filter() {
		int rowCount = getDataProvider().getTotalRowCount();
		if (rowCount == 0) return;

		checkProtocol();

		if (feature != null && !fClasses.isEmpty()) {
			BitSet currentFilter = getDataProvider().getCurrentFilter();
			IntStream.range(0, rowCount).forEach(index -> {
				if (currentFilter.get(index)) {
					Well w = getDataProvider().getCurrentItems().get(index);
					currentFilter.set(index, ClassificationService.getInstance().matchesClasses(
							w, feature, fClasses, showNotClassified));
				}
			});
		}
	}

	@Override
	public boolean isActive() {
		return feature != null && !fClasses.isEmpty() && !fClasses.equals(feature.getFeatureClasses());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setProperties(Object o) {
		if (o instanceof HashMap<?, ?>) {
			Map<String, Object> properties = (Map<String, Object>) o;
			// Set selected Feature
			String featureName = (String) properties.get(FEATURE);
			if (featureName != null) {
				checkProtocol();
				for (Feature f : features) {
					if (f.getName().equals(featureName) || f.getShortName().equals(featureName)) {
						feature = f;

						fClasses.clear();
						List<String> filterClassNames = (List<String>) properties.get(FEATURE_CLASS_NAMES);
						for (FeatureClass fc : feature.getFeatureClasses()) {
							if (filterClassNames.contains(fc.getLabel())) {
								fClasses.add(fc);
							}
						}

						break;
					}
				}
				showNotClassified = (boolean) properties.get(SHOW_NOT_CLASSIFIED);
			}
		}
	}

	@Override
	public Object getProperties() {
		List<String> featureClassNames = new ArrayList<>();
		for (FeatureClass featureClass : fClasses) {
			featureClassNames.add(featureClass.getLabel());
		}

		Map<String, Object> properties = new HashMap<>();
		if (feature != null) properties.put(FEATURE, feature.getName());
		properties.put(FEATURE_CLASS_NAMES, featureClassNames);
		properties.put(SHOW_NOT_CLASSIFIED, showNotClassified);

		return properties;
	}

	private void checkProtocol() {
		// Check if a different protocol was selected.
		Plate key = getDataProvider().getKey(0);
		if (key == null) return;
		ProtocolClass pc = PlateUtils.getProtocolClass(key);
		if (!pc.equals(pClass)) {
			pClass = pc;

			features = ClassificationService.getInstance().findWellClassificationFeatures(pClass);
			if (!features.isEmpty()) {
				feature = features.get(0);
				fClasses = new ArrayList<>(feature.getFeatureClasses());
			} else {
				feature = null;
				fClasses.clear();
			}
		}
	}

}
