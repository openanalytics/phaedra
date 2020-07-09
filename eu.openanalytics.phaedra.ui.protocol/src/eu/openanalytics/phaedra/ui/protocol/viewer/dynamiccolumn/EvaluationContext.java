package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncData1toNViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolClasses;


public class EvaluationContext<TEntity> {
	
	
	public static interface Listener {
		
		/**
		 * Called if the context changed.
		 * 
		 * Thread: display thread only.
		 * 
		 * @param properties the changed properties
		 */
		void onContextChanged(final Set<String> properties);
		
	}
	
	private static class ProtocolClassData {
		
		public static ProtocolClassData create(final ProtocolUIService uiService, final ProtocolClass protocolClass) {
			final Feature feature = uiService.getCurrentFeature(protocolClass);
			final String normalization = uiService.getCurrentNormalization(feature);
			return new ProtocolClassData(feature, normalization);
		}
		
		private final Feature feature;
		private final String normalization;
		
		public ProtocolClassData(final Feature feature, final String normalization) {
			this.feature = feature;
			this.normalization = normalization;
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof ProtocolClassData) {
				final ProtocolClassData other = (ProtocolClassData)obj;
				return (Objects.equals(this.feature, other.feature)
						&& Objects.equals(this.normalization, other.normalization) );
			}
			return false;
		}
		
	}
	
	private static class Context {
		
		private final Set<ProtocolClass> protocolClasses;
		
		private final HashMap<ProtocolClass, ProtocolClassData> features = new HashMap<>();
		
		public Context(final Set<ProtocolClass> protocolClasses) {
			this.protocolClasses = protocolClasses;
		}
		
	}
	
	
	private final AsyncDataViewerInput<TEntity, ?> viewerInput;
	private final ProtocolClasses<TEntity> protocolClasses;
	
	private final IUIEventListener protocolUiListener;
	
	private volatile Context current;
	
	private final ListenerList<Listener> listeners = new ListenerList<>(ListenerList.IDENTITY);
	private final NotifyListenerRunnable notifyListeners = new NotifyListenerRunnable();
	
	
	public EvaluationContext(final AsyncDataViewerInput<TEntity, ?> viewerInput,
			final ProtocolClasses<TEntity> protocolClasses) {
		this.viewerInput = viewerInput;
		this.protocolClasses = protocolClasses;
		this.current = new Context(Collections.emptySet());
		this.protocolUiListener = this::handleProtocolUiEvent;
		ProtocolUIService.getInstance().addUIEventListener(this.protocolUiListener);
		
		if (this.protocolClasses != null) {
			this.protocolClasses.addValueChangeListener((event) -> {
				updateContext(event.diff.getNewValue());
			});
			updateContext(this.protocolClasses.getValue());
		}
	}
	
	public void dispose() {
		ProtocolUIService.getInstance().removeUIEventListener(this.protocolUiListener);
	}
	
	
	public void addListener(final Listener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(final Listener listener) {
		this.listeners.remove(listener);
	}
	
	
	private class NotifyListenerRunnable implements Runnable {
		
		private boolean isScheduled;
		
		private final Set<String> changedProperties = new HashSet<String>();
		
		public void schedule() {
			if (!this.isScheduled) {
				this.isScheduled = true;
				Display.getDefault().asyncExec(this);
			}
		}
		
		@Override
		public void run() {
			this.isScheduled = false;
			for (final Listener listener : EvaluationContext.this.listeners) {
				listener.onContextChanged(this.changedProperties);
			}
			this.changedProperties.clear();
		}
		
	}
	
//	private void handleProtocolClassesEvent(final ValueChangeEvent<? extends Set<ProtocolClass>> event) {
//		this.current = createContext(event.diff.getNewValue());
//	}
	
	private void updateContext(final Set<ProtocolClass> protocolClasses) {
		final Context context = new Context(protocolClasses);
		final ProtocolUIService protocolUIService = ProtocolUIService.getInstance();
		for (final ProtocolClass protocolClass : context.protocolClasses) {
			final ProtocolClassData data = ProtocolClassData.create(protocolUIService, protocolClass);
			context.features.put(protocolClass, data);
		}
		this.current = context;
	}
	
	private void handleProtocolUiEvent(final UIEvent event) {
		final Context context = this.current;
		final ProtocolUIService protocolUIService;
		final ProtocolClass protocolClass;
		switch (event.type) {
		case FeatureSelectionChanged:
			protocolUIService = ProtocolUIService.getInstance();
			protocolClass = protocolUIService.getCurrentProtocolClass();
			if (protocolClass != null && context.protocolClasses.contains(protocolClass)) {
				final ProtocolClassData data = ProtocolClassData.create(protocolUIService, protocolClass);
				final ProtocolClassData previousData = context.features.replace(protocolClass, data);
				if (!Objects.equals(data, previousData)) {
					this.notifyListeners.changedProperties.add("Feature");
					if (previousData == null || !Objects.equals(data.normalization, previousData.normalization)) {
						this.notifyListeners.changedProperties.add("FeatureNormalization");
					}
					this.notifyListeners.changedProperties.add("FeatureValue");
					this.notifyListeners.changedProperties.add("ColorMethod");
					this.notifyListeners.schedule();
				}
			}
			return;
		case NormalizationSelectionChanged:
			protocolUIService = ProtocolUIService.getInstance();
			protocolClass = protocolUIService.getCurrentProtocolClass();
			if (protocolClass != null && context.protocolClasses.contains(protocolClass)) {
				final ProtocolClassData data = ProtocolClassData.create(protocolUIService, protocolClass);
				final ProtocolClassData previousData = context.features.replace(protocolClass, data);
				if (previousData == null || !Objects.equals(data.normalization, previousData.normalization)) {
					this.notifyListeners.changedProperties.add("FeatureNormalization");
					this.notifyListeners.changedProperties.add("FeatureValue");
					this.notifyListeners.schedule();
				}
			}
			return;
		case ColorMethodChanged:
			// plate/experiment limit is not per protocol class
			this.notifyListeners.changedProperties.add("ColorMethod");
			this.notifyListeners.schedule();
			return;
		default:
			return;
		}
	}
	
	
	public List<ScriptLanguage> getScriptLanguages() {
		return DynamicColumns.DEFAULT_SCRIPT_LANGUAGES;
	}
	
	public String getScriptNote() {
		return null;
	}
	
	
	/**
	 * Returns the current feature for the specified protocol class.
	 * 
	 * Thread: any thread.
	 * 
	 * @param protocolClass
	 * @return the current feature or <code>null</code>
	 */
	public Feature getFeature(final ProtocolClass protocolClass) {
		final Context context = this.current;
		final ProtocolClassData data = context.features.get(protocolClass);
		return (data != null) ? data.feature : null;
	}
	
	/**
	 * Returns the current feature normalization for the specified protocol class.
	 * 
	 * Thread: any thread.
	 * 
	 * @param protocolClass
	 * @return the current normalization or <code>null</code>
	 */
	public String getNormalization(final ProtocolClass protocolClass) {
		final Context context = this.current;
		final ProtocolClassData data = context.features.get(protocolClass);
		return (data != null) ? data.normalization : null;
	}
	
	
	/**
	 * Returns the current feature for the specified element.
	 * 
	 * Thread: any thread.
	 * 
	 * @param element
	 * @return the current feature or <code>null</code>
	 */
	public Feature getFeature(final TEntity element) {
		return getFeature(this.protocolClasses.getProtocolClass(element));
	}
	
	/**
	 * Returns the current feature normalization for the specified element.
	 * 
	 * Thread: any thread.
	 * 
	 * @param element
	 * @return the current normalization or <code>null</code>
	 */
	public String getNormalization(final TEntity element) {
		return getNormalization(this.protocolClasses.getProtocolClass(element));
	}
	
	
	/**
	 * Adds variables to the script context.
	 * 
	 * Thread: any thread.
	 */
	public void contributeVariables(final ScriptContext context, final TEntity element) {
		if (this.viewerInput != null) {
			addInputElementVariables(context, this.viewerInput, element);
		}
		if (this.protocolClasses != null) {
			addFeatureVariables(context, element);
		}
	}
	
	protected void addInputElementVariables(final ScriptContext context,
			final AsyncDataViewerInput<TEntity, ?> viewerInput, final TEntity element) {
		if (IValueObject.class.isAssignableFrom(viewerInput.getBaseElementType())) {
			context.putValueObject(viewerInput.getBaseElementType(), null,
					(IValueObject)element );
		}
		if (viewerInput instanceof AsyncData1toNViewerInput) {
			context.putValueObjects(viewerInput.getViewerElementType(), null,
					(context.isEditorContext()) ? null : ((AsyncData1toNViewerInput)viewerInput).getViewerElements(element) );
		}
	}
	
	private static final BiFunction<Well, Object, Double> GET_FEATURE_VALUE_FUNCTION = new BiFunction<Well, Object, Double>() {
		@Override
		public Double apply(final Well well, final Object featureRef) {
			final ProtocolClass protocolClass = well.getPlate().getExperiment().getProtocol().getProtocolClass();
			final Feature feature;
			if (featureRef != null) {
				if (featureRef instanceof Feature) {
					feature = (Feature)featureRef;
				}
				else if (featureRef instanceof String) {
					feature = ProtocolUtils.getFeatureByName((String)featureRef, protocolClass);
				}
				else {
					throw new IllegalArgumentException("feature.class= " + featureRef.getClass());
				}
			}
			else {
				feature = protocolClass.getDefaultFeature();
			}
			if (feature == null) {
				return Double.NaN;
			}
			return CalculationService.getInstance().getAccessor(well.getPlate())
					.getNumericValue(well, feature, null);
		}
	};
	
	protected void addFeatureVariables(final ScriptContext context, final TEntity element) {
		context.putValueObject("feature", "feature : Feature", "the current feature",
				(context.isEditorContext()) ? null : getFeature(element) );
		context.putString("normalization", "the current normalization",
				(context.isEditorContext()) ? null : getNormalization(element) );
		context.putFunction("getValue", "getValue(well, feature) : double", "gets the numeric feature value",
				GET_FEATURE_VALUE_FUNCTION );
	}
	
}
