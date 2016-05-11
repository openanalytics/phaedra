package eu.openanalytics.phaedra.ui.subwell.chart.v2.filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.AbstractFilter;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.plate.chart.v2.filter.ClassificationFilterDialog;
import eu.openanalytics.phaedra.ui.subwell.Activator;

public class ClassificationFilter extends AbstractFilter<Well, Well> {

	private static final ExecutorService executor = Executors.newFixedThreadPool(PrefUtils.getNumberOfThreads());

	private static final String FEATURE = "FEATURE";
	private static final String SHOW_NOT_CLASSIFIED = "SHOW_NOT_CLASSIFIED";
	private static final String SHOW_REJECTED = "SHOW_REJECTED";
	private static final String FEATURE_CLASS_NAMES = "FEATURE_CLASS_NAMES";

	private ProtocolClass pClass;
	private List<SubWellFeature> features;
	private SubWellFeature feature;
	private List<FeatureClass> fClasses;
	private boolean showNotClassified;
	private boolean showRejected;
	private SubWellFeature rejectionFeature;

	public ClassificationFilter(IDataProvider<Well, Well> dataProvider) {
		super("Classification", dataProvider);

		this.features = new ArrayList<>();
		this.fClasses = new ArrayList<>();
		this.showNotClassified = true;
		this.showRejected = true;
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
		ClassificationFilterDialog<SubWellFeature> dialog = new ClassificationFilterDialog<>(
				Display.getDefault().getActiveShell()
				, features, feature, fClasses, showNotClassified, showRejected, rejectionFeature);

		if (dialog.open() == Window.OK) {
			feature = dialog.getFeature();
			fClasses = dialog.getFeatureClasses();
			showNotClassified = dialog.isShowNotClassified();
			showRejected = dialog.isShowRejected();

			super.doApplyFilterItem(filterItem);
		}
	}

	@Override
	public void filter() {
		int rowCount = getDataProvider().getTotalRowCount();
		if (rowCount == 0) return;

		checkProtocol();

		if (feature != null && !fClasses.isEmpty()) {
			final Map<Well, Future<BitSet>> results = new HashMap<Well, Future<BitSet>>();

			final List<FeatureClass> rejectedClass;
			if (!showRejected && rejectionFeature != null) {
				rejectedClass = new ArrayList<>();
				rejectedClass.add(ClassificationService.getInstance().findRejectionClass(rejectionFeature));
			} else {
				rejectedClass = null;
			}

			for (final Well well : getDataProvider().getCurrentEntities()) {
				final int size = getDataProvider().getDataSizes().get(well);
				results.put(well, executor.submit(new Callable<BitSet>() {
					@Override
					public BitSet call() throws Exception {
						BitSet bitSet = new BitSet(size);
						for (int i = 0; i < size; i++) {
							if (rejectedClass != null) {
								boolean isRejected = ClassificationService.getInstance().matchesClasses(
										well, i, rejectionFeature, rejectedClass, false);

								bitSet.set(i, !isRejected);
								if (isRejected) continue;
							}
							bitSet.set(i, ClassificationService.getInstance().matchesClasses(
									well, i, feature, fClasses, showNotClassified));
						}
						return bitSet;
					}
				}));
			}

			BitSet currentFilter = getDataProvider().getCurrentFilter();
			for (Entry<Well, Future<BitSet>> result : results.entrySet()) {
				try {
					BitSet tempBit = result.getValue().get();
					int[] range = getDataProvider().getKeyRange(result.getKey());
					for (int i = range[0]; i <= range[1]; i++) {
						if (currentFilter.get(i)) {
							currentFilter.set(i, tempBit.get(i - range[0]));
						}
					}
				} catch (InterruptedException | ExecutionException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
			}
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
				for (SubWellFeature f : features) {
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
				showRejected = (boolean) properties.get(SHOW_REJECTED);
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
		properties.put(SHOW_REJECTED, showRejected);

		return properties;
	}

	private void checkProtocol() {
		// Check if a different protocol was selected.
		Well key = getDataProvider().getKey(0);
		if (key == null) return;
		ProtocolClass pc = PlateUtils.getProtocolClass(key);
		if (!pc.equals(pClass)) {
			pClass = pc;

			features = ClassificationService.getInstance().findSubWellClassificationFeatures(pClass);
			if (!features.isEmpty()) {
				feature = features.get(0);
				fClasses = new ArrayList<>(feature.getFeatureClasses());
				rejectionFeature = ClassificationService.getInstance().findRejectionFeature(pClass);
				showRejected = rejectionFeature != null;
			} else {
				feature = null;
				fClasses.clear();
				rejectionFeature = null;
				showRejected = true;
			}
		}
	}

}