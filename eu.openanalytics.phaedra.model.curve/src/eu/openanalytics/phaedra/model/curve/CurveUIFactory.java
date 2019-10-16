package eu.openanalytics.phaedra.model.curve;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.ObservableMap;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.model.curve.CurveParameter.ParameterType;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveUIFactory {

	/**
	 * Create a set of fields for editing curve settings.
	 * This method supports two mechanisms:
	 * <ul>
	 * <li>To edit a feature's default settings, provide a bindingCtx. Changes will be applied to the feature's settings map.</li>
	 * <li>To edit a curve's custom settings, provide a customSettings instance. Changes will be applied to the object.</li>
	 * </ul>
	 * In both cases, the dirtyListener will be called whenever a setting is changed.
	 * 
	 * @param parent The parent Composite where the widget will be added to.
	 * @param feature The Feature where the curve belongs to.
	 * @param customSettings Optional, custom settings for a single curve.
	 * @param bindingCtx Optional, to bind the fields to the feature's settings.
	 * @param dirtyListener An optional listener for change events.
	 * @return The child Composite containing the added widgets.
	 */
	public static Composite createFields(Composite parent, Feature feature, CurveFitSettings customSettings, DataBindingContext bindingCtx, Listener dirtyListener) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(area);
		
		Link clearSettingsLnk = new Link(area, SWT.NONE);
		clearSettingsLnk.setText("<a>Clear settings</a>");
		GridDataFactory.fillDefaults().span(2,1).applyTo(clearSettingsLnk);
		
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Model:");
		
		CCombo modelCmb = new CCombo(area, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(modelCmb);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Description:");
		
		Label modelDescriptionLbl = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(modelDescriptionLbl);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Grouping:");
		
		Composite groupingCmp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupingCmp);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(groupingCmp);
		
		CCombo groupBy1Cmb = new CCombo(groupingCmp, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupBy1Cmb);
		
		new Label(groupingCmp, SWT.NONE).setText(">");
		CCombo groupBy2Cmb = new CCombo(groupingCmp, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupBy2Cmb);
		
		new Label(groupingCmp, SWT.NONE).setText(">");
		CCombo groupBy3Cmb = new CCombo(groupingCmp, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupBy3Cmb);
		
		Composite additionParamCmp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(additionParamCmp);
		{	GridLayout layout = GridLayoutFactory.fillDefaults().numColumns(2).create();
			layout.horizontalSpacing = Math.max(layout.horizontalSpacing, 8);
			additionParamCmp.setLayout(layout);
		}

		// Input & listeners
		CurveSettingsMap observableMap = (customSettings == null) ? new CurveSettingsMap(feature.getCurveSettings()) : null;
		
		String[] models = CurveFitService.getInstance().getFitModels();
		String[] allModels = new String[models.length + 1];
		allModels[0] = "";
		System.arraycopy(models, 0, allModels, 1, models.length);
		
		List<String> annotationList = ProtocolService.streamableList(feature.getProtocolClass().getFeatures()).stream()
				.filter(f -> f.isAnnotation())
				.map(f -> f.getName())
				.sorted()
				.collect(Collectors.toList());
		annotationList.add(0, "");
		String[] annotations = annotationList.toArray(new String[annotationList.size()]);
		
		modelCmb.setItems(allModels);
		groupBy1Cmb.setItems(annotations);
		groupBy2Cmb.setItems(annotations);
		groupBy3Cmb.setItems(annotations);
		
		if (bindingCtx != null && observableMap != null) {
			FormEditorUtils.bindSelectionToMap(modelCmb, observableMap, CurveFitSettings.MODEL, bindingCtx);
			FormEditorUtils.bindSelectionToMap(groupBy1Cmb, observableMap, CurveFitSettings.GROUP_BY_1, bindingCtx);
			FormEditorUtils.bindSelectionToMap(groupBy2Cmb, observableMap, CurveFitSettings.GROUP_BY_2, bindingCtx);
			FormEditorUtils.bindSelectionToMap(groupBy3Cmb, observableMap, CurveFitSettings.GROUP_BY_3, bindingCtx);
		} else if (customSettings != null) {
			modelCmb.select(modelCmb.indexOf(customSettings.getModelId()));
			modelCmb.addListener(SWT.Selection, event -> customSettings.setModelId(modelCmb.getText()));
		}
		
		if (dirtyListener != null) {
			modelCmb.addListener(SWT.Selection, dirtyListener);
			groupBy1Cmb.addListener(SWT.Selection, dirtyListener);
			groupBy2Cmb.addListener(SWT.Selection, dirtyListener);
			groupBy3Cmb.addListener(SWT.Selection, dirtyListener);
		}
		
		SelectionAdapter onModelSelection = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control child: additionParamCmp.getChildren()) child.dispose();
				
				String selectedModelId = modelCmb.getText();
				if (selectedModelId.isEmpty()) {
					modelDescriptionLbl.setText("");
				} else {
					ICurveFitModel model = CurveFitService.getInstance().getModel(selectedModelId);
					modelDescriptionLbl.setText(model.getDescription());
					
					for (CurveParameter.Definition param: model.getInputParameters()) {
						Value value;
						if (customSettings != null) {
							value = CurveParameter.find(customSettings.getExtraParameters(), param.name);
							if (value == null) continue;
						} else value = null;
						
						Label lbl = new Label(additionParamCmp, SWT.NONE);
						lbl.setText(param.name + ":");
						lbl.setToolTipText(param.description);
						
						if (param.valueRestriction instanceof CurveParameter.ParameterValueList) { // combo
							String[] allowedValues = ((CurveParameter.ParameterValueList)param.valueRestriction).getAllowedValues();
							CCombo cmb = new CCombo(additionParamCmp, SWT.BORDER | SWT.READ_ONLY);
							GridDataFactory.fillDefaults().grab(true, false).applyTo(cmb);
							cmb.setItems(allowedValues);
							
							if (bindingCtx != null) {
								UpdateValueStrategy targetToModel = new UpdateValueStrategy();
								if (param.valueRestriction instanceof IValidator) {
									targetToModel.setAfterGetValidator((IValidator)param.valueRestriction);
								}
								Binding bindValue = bindingCtx.bindValue(WidgetProperties.selection().observe(cmb),
										(value != null) ?
												new ParamValueObservable(value) :
												Observables.observeMapEntry(observableMap, param.name),
										targetToModel, null);
								ControlDecorationSupport.create(bindValue, SWT.TOP | SWT.LEFT);
							}
							else if (value != null) {
								cmb.select(cmb.indexOf(CurveParameter.getValueAsString(value)));
								cmb.addModifyListener(event -> CurveParameter.setValueFromString(value, cmb.getText()));
							}
							if (dirtyListener != null) cmb.addModifyListener(event -> dirtyListener.handleEvent(null));
							cmb.requestLayout();
						}
						else { // text field
							Text txt = new Text(additionParamCmp, SWT.BORDER);
							GridDataFactory.fillDefaults().grab(true, false).applyTo(txt);
							
							if (bindingCtx != null) {
								UpdateValueStrategy targetToModel = new UpdateValueStrategy();
								if (param.valueRestriction instanceof IValidator) {
									targetToModel.setAfterGetValidator((IValidator)param.valueRestriction);
								}
								else if (param.type == ParameterType.Numeric) {
									targetToModel.setAfterGetValidator(new CurveParameter.NumericValueValidator(param.name));
								}
								if (param.valueRestriction instanceof IConverter) {
									targetToModel.setConverter((IConverter)param.valueRestriction);
								}
								Binding bindValue = bindingCtx.bindValue(WidgetProperties.text(SWT.Modify).observe(txt),
										(value != null) ?
												new ParamValueObservable(value) :
												Observables.observeMapEntry(observableMap, param.name),
										targetToModel, null);
								ControlDecorationSupport.create(bindValue, SWT.TOP | SWT.LEFT);
							}
							else if (value != null) {
								String s = CurveParameter.getValueAsString(value);
								if (s != null) txt.setText(s);
								txt.addModifyListener(event -> CurveParameter.setValueFromString(value, txt.getText()));
							}
							if (dirtyListener != null) txt.addModifyListener(event -> dirtyListener.handleEvent(null));
							txt.requestLayout();
						}
					}
				}
			};
		};
		modelCmb.addSelectionListener(onModelSelection);
		
		clearSettingsLnk.addListener(SWT.Selection, e -> {
			modelCmb.select(0);
			groupBy1Cmb.select(0);
			groupBy2Cmb.select(0);
			groupBy3Cmb.select(0);
			onModelSelection.widgetSelected(null);
			if (dirtyListener != null) dirtyListener.handleEvent(null);
		});
		
		onModelSelection.widgetSelected(null);
		return area;
	}
	
	
	private static class CurveSettingsMap extends ObservableMap<String, String> {
		
		public CurveSettingsMap(Map<String, String> wrappedMap) {
			super(wrappedMap);
		}
		
		@Override
		public String put(String key, String value) {
			// Fix issue with "" values becoming NULL values in DB
			if (value == null || value.isEmpty()) return remove(key);
			return wrappedMap.put(key, value);
		}

		@Override
		public String remove(Object key) {
			return wrappedMap.remove(key);
		}

		@Override
		public void clear() {
			wrappedMap.clear();
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {
			wrappedMap.putAll(m);
		}
	}
	
	private static class ParamValueObservable extends AbstractObservableValue<String> {
		
		private final CurveParameter.Value paramValue;
		
		public ParamValueObservable(CurveParameter.Value paramValue) {
			this.paramValue = paramValue;
		}
		
		@Override
		public Object getValueType() {
			return String.class;
		}
		
		@Override
		protected String doGetValue() {
			return CurveParameter.getValueAsString(this.paramValue);
		}
		
		@Override
		protected void doSetValue(String value) {
			CurveParameter.setValueFromString(this.paramValue, value);
		}
		
	}
	
}
