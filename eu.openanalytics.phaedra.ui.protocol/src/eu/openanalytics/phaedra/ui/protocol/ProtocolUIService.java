package eu.openanalytics.phaedra.ui.protocol;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.FeatureGroupManager;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class ProtocolUIService extends EventManager implements IFeatureProvider {

	private ProtocolClass currentProtocolClass;
	private Map<ProtocolClass, Feature> currentFeatures;
	private Map<ProtocolClass, FeatureGroup> currentFeatureGroups;
	private Map<Feature, String> currentNormalizations;
	private IColorMethod colorMethod;

	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;

	private static ProtocolUIService instance;

	private boolean isExperimentLimit;

	private ProtocolUIService() {
		// Initialize ProtocolService first for the ModelEventListener that is added in the constructor.
		ProtocolService.getInstance();

		currentFeatures = new HashMap<>();
		currentFeatureGroups = new HashMap<>();
		currentNormalizations = new HashMap<>();

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				ProtocolClass pClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
				if (pClass != null && !pClass.equals(currentProtocolClass)) {
					// A new protocol class is selected: set as current and fire event.
					setCurrentProtocolClass(pClass);
				}
			}
		};

		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window.getActivePage() == null) {
				window.addPageListener(new IPageListener() {
					@Override
					public void pageOpened(IWorkbenchPage page) {
						page.addSelectionListener(selectionListener);
					}
					@Override
					public void pageClosed(IWorkbenchPage page) {
						// Do nothing.
					}
					@Override
					public void pageActivated(IWorkbenchPage page) {
						// Do nothing.
					}
				});
			} else {
				window.getActivePage().addSelectionListener(selectionListener);
			}
		} catch (IllegalStateException e) {
			// In headless mode, ignore this error.
		}

		modelEventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
					ProtocolClass pc = (ProtocolClass)event.source;
					if (currentProtocolClass != null && currentProtocolClass.equals(pc)) {
						// Color method config may have changed.
						if (getCurrentFeature() != null) colorMethod = ColorMethodFactory.createColorMethod(getCurrentFeature());
						else {
							colorMethod = ColorMethodRegistry.getInstance().getDefaultColorMethod();
							colorMethod.configure(null);
						}
					}
				}
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);

		// Load & save state
		PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				saveState();
				return true;
			}
			@Override
			public void postShutdown(IWorkbench workbench) {
				// Do nothing
			}
		});

		try {
			loadState();
		} catch (Exception e) {
			// May fail when switching between environments: nonexisting protocol class ids.
			EclipseLog.warn("Failed to restore state for " + this.getClass().getSimpleName(), Activator.getDefault());
		}
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public static ProtocolUIService getInstance() {
		if (instance == null) instance = new ProtocolUIService();
		return instance;
	}

	public void post(UIEvent event) {
		fire(event);
	}

	@Override
	public ProtocolClass getCurrentProtocolClass() {
		return currentProtocolClass;
	}

	public void setCurrentProtocolClass(ProtocolClass newProtocolClass) {
		if (newProtocolClass == null || newProtocolClass.equals(currentProtocolClass)) return;

		Feature f = currentFeatures.get(newProtocolClass);
		if (f == null) f = newProtocolClass.getDefaultFeature();
		if (f == null && !newProtocolClass.getFeatures().isEmpty()) {
			f = newProtocolClass.getFeatures().get(0);
		}
		setCurrentFeature(f);
	}

	@Override
	public Feature getCurrentFeature() {
		if (currentProtocolClass == null) return null;
		return currentFeatures.get(currentProtocolClass);
	}

	public void setCurrentFeature(Feature f) {
		if (f == null || f.equals(getCurrentFeature())) return;

		ProtocolClass pClass = f.getProtocolClass();
		if (!pClass.equals(currentProtocolClass)) currentProtocolClass = pClass;

		colorMethod = ColorMethodFactory.createColorMethod(f);
		currentFeatures.put(f.getProtocolClass(), f);

		updateFeatureGroup(null);

		String normalization = getNormalization(f);
		currentNormalizations.put(f, normalization);

		UIEvent event = new UIEvent(EventType.FeatureSelectionChanged);
		fire(event);

		event = new UIEvent(EventType.NormalizationSelectionChanged);
		fire(event);
	}

	@Override
	public String getCurrentNormalization() {
		Feature f = getCurrentFeature();
		if (f == null) return null;
		return currentNormalizations.get(f);
	}

	public void setCurrentNormalization(String normalization) {
		if (normalization == null || getCurrentFeature() == null || normalization.equals(getCurrentNormalization())) {
			return;
		}
		currentNormalizations.put(getCurrentFeature(), normalization);
		UIEvent event = new UIEvent(EventType.NormalizationSelectionChanged);
		fire(event);
	}

	public FeatureGroup getCurrentFeatureGroup() {
		if (currentProtocolClass == null) return null;
		return currentFeatureGroups.get(currentProtocolClass);
	}

	public void setCurrentFeatureGroup(FeatureGroup group) {
		if (group == null || group.equals(getCurrentFeatureGroup())) return;
		updateFeatureGroup(group);
	}

	@Override
	public boolean isExperimentLimit() {
		return isExperimentLimit;
	}

	public void setExperimentLimit(boolean isExperimentLimit) {
		this.isExperimentLimit = isExperimentLimit;
		fire(new UIEvent(EventType.ColorMethodChanged));
	}

	@Override
	public IColorMethod getCurrentColorMethod() {
		return colorMethod;
	}

	/*
	 * **************
	 * Event handling
	 * **************
	 */

	@Override
	public void addUIEventListener(IUIEventListener listener) {
		addListenerObject(listener);
	}

	@Override
	public void removeUIEventListener(IUIEventListener listener) {
		removeListenerObject(listener);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void fire(UIEvent event) {
		for (Object listener: getListeners()) {
			((IUIEventListener)listener).handle(event);
		}
	}

	private String getNormalization(Feature f) {
		String normalization = currentNormalizations.get(f);
		if (normalization == null) {
			normalization = f.getNormalization();
		}
		return normalization;
	}

	private void updateFeatureGroup(FeatureGroup newGroup) {
		if (currentProtocolClass == null) return;
		if (newGroup != null && newGroup.equals(getCurrentFeatureGroup())) return;

		if (newGroup == null) {
			Feature currentFeature = getCurrentFeature();
			FeatureGroup currentGroup = getCurrentFeatureGroup();
			if (ProtocolService.getInstance().isMember(currentFeature, currentGroup)) return; // The current group is just fine.

			// Select an appropriate new group: KEY or ALL
			newGroup = ProtocolService.getInstance().getFeatureGroup(currentProtocolClass, GroupType.WELL, FeatureGroupManager.FEATURE_GROUP_KEY);
			if (!ProtocolService.getInstance().isMember(currentFeature, newGroup)) {
				newGroup = ProtocolService.getInstance().getFeatureGroup(currentProtocolClass, GroupType.WELL, FeatureGroupManager.FEATURE_GROUP_ALL);
			}
			currentFeatureGroups.put(currentProtocolClass, newGroup);
			fire(new UIEvent(EventType.FeatureGroupSelectionChanged));
		} else {
			// A group is explicitly given. Update current feature if necessary.
			currentFeatureGroups.put(currentProtocolClass, newGroup);
			fire(new UIEvent(EventType.FeatureGroupSelectionChanged));
			Feature currentFeature = getCurrentFeature();
			if (!ProtocolService.getInstance().isMember(currentFeature, newGroup)) {
				Feature newFeature = CollectionUtils.getFirstElement(ProtocolService.getInstance().getMembers(newGroup));
				setCurrentFeature(newFeature);
			}
		}
	}

	private void saveState() {
		XMLMemento memento = XMLMemento.createWriteRoot("ProtocolUIState");
		if (currentProtocolClass != null) memento.putString("pc", "" + currentProtocolClass.getId());
		if (currentFeatures != null) {
			for (ProtocolClass pc: currentFeatures.keySet()) {
				if (currentFeatures.get(pc) == null) continue;
				IMemento child = memento.createChild("f");
				child.putString("pcId", "" + pc.getId());
				child.putString("id", "" + currentFeatures.get(pc).getId());
			}
		}
		if (currentFeatureGroups != null) {
			for (ProtocolClass pc: currentFeatureGroups.keySet()) {
				if (currentFeatureGroups.get(pc) == null) continue;
				IMemento child = memento.createChild("fg");
				child.putString("pcId", "" + pc.getId());
				child.putString("id", "" + currentFeatureGroups.get(pc).getId());
			}
		}
		if (currentNormalizations != null) {
			for (Feature f: currentNormalizations.keySet()) {
				if (f == null) continue;
				IMemento child = memento.createChild("n");
				child.putString("fId", "" + f.getId());
				child.putString("norm", currentNormalizations.get(f));
			}
		}

		JobUtils.runBackgroundJob(() -> {
			try {
				StringWriter writer = new StringWriter();
				memento.save(writer);
				UserService.getInstance().setPreferenceValue("Memento", "ProtocolUIState", writer.toString());
			} catch (Exception e) {
				EclipseLog.error("Failed to save ProtocolUI state", e, Activator.getDefault());
			}
		});
	}

	private void loadState() {
		String value = UserService.getInstance().getPreferenceValue("Memento", "ProtocolUIState");
		if (value == null || value.isEmpty()) return;
		XMLMemento memento = null;
		try {
			memento = XMLMemento.createReadRoot(new StringReader(value));
		} catch (Exception e) {
			EclipseLog.error("Failed to restore ProtocolUI state", e, Activator.getDefault());
			return;
		}

		// Disabled: poor performance, as this may trigger hundreds of small DB queries
//		IMemento[] features = memento.getChildren("f");
//		for (IMemento f: features) {
//			ProtocolClass pc = ProtocolService.getInstance().getProtocolClass(Long.parseLong(f.getString("pcId")));
//			Feature feature = ProtocolService.getInstance().getFeature(Long.parseLong(f.getString("id")));
//			currentFeatures.put(pc, feature);
//		}
//
//		IMemento[] featureGroups = memento.getChildren("fg");
//		for (IMemento fg: featureGroups) {
//			ProtocolClass pc = ProtocolService.getInstance().getProtocolClass(Long.parseLong(fg.getString("pcId")));
//			long groupId = Long.parseLong(fg.getString("id"));
//			FeatureGroup group = CollectionUtils.find(pc.getFeatureGroups(), g -> g.getId() == groupId);
//			currentFeatureGroups.put(pc, group);
//		}

		// Disabled: by saving active normalizations, changes in the protocol class could not be seen anymore.
//		IMemento[] normalizations = memento.getChildren("n");
//		for (IMemento n: normalizations) {
//			Feature f = ProtocolService.getInstance().getFeature(Long.parseLong(n.getString("fId")));
//			String norm = n.getString("norm");
//			currentNormalizations.put(f, norm);
//		}

		String pcId = memento.getString("pc");
		if (pcId != null) setCurrentProtocolClass(ProtocolService.getInstance().getProtocolClass(Long.parseLong(pcId)));
	}
}