package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.CONDITIONAL_FORMAT_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.EXPRESSION_CODE_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.EXPRESSION_LANGUAGE_ID_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.FORMULA_ID_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.PREDEFINED_FORMULA;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.SPECIFIED_EXPRESSION;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_DATA_TYPE_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_FORMAT_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_SUPPLIER_KEY;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomColumnSupport;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomDataUtils;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.EditCustomColumnDialog;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.CustomDataFormatSupplier;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DataProvider;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnLabelProvider;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicConditionalLabelProvider;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.ExpressionDataProvider;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.FormulaDataProvider;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit.ConditionalFormattingTab;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit.FormattingTab;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit.ValueTab;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat.FormatConfig;


public class DynamicColumnSupport<TEntity, TViewerElement> extends CustomColumnSupport {
	
	
	private final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput;
	
	private final Supplier<DataFormatter> dataFormatSupplier;
	
	private final EvaluationContext<TEntity> evaluationContext;
	private final boolean isFormulaSupported;
	
	private final List<? extends ValueDataType> valueDataTypes;
	
	private final List<? extends ConditionalFormat> conditionalFormats;
	
	
	public DynamicColumnSupport(final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput,
			final EvaluationContext<TEntity> evaluationContext,
			final Supplier<DataFormatter> dataFormatSupplier,
			final List<? extends ValueDataType> valueDataTypes,
			final List<? extends ConditionalFormat> conditionalFormats) {
		this.viewerInput = viewerInput;
		this.evaluationContext = evaluationContext;
		this.dataFormatSupplier = dataFormatSupplier;
		
		this.isFormulaSupported = (viewerInput.getBaseElementType() == Plate.class && viewerInput.getViewerElementType() == Well.class);
		this.valueDataTypes = valueDataTypes;
		this.conditionalFormats = conditionalFormats;
		
		this.evaluationContext.addListener(new EvaluationContext.Listener() {
			@Override
			public void onContextChanged(final Set<String> properties) {
				viewerInput.reload(properties);
			}
		});
	}
	
	public DynamicColumnSupport(final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput,
			final EvaluationContext<TEntity> evaluationContext,
			final Supplier<DataFormatter> dataFormatSupplier) {
		this(viewerInput, evaluationContext, dataFormatSupplier,
				DynamicColumns.DEFAULT_TYPES, DynamicColumns.DEFAULT_CONDITIONAL_FORMATS);
	}
	
	
	public AsyncDataViewerInput<TEntity, TViewerElement> getViewerInput() {
		return this.viewerInput;
	}
	
	public EvaluationContext<TEntity> getEvaluationContext() {
		return this.evaluationContext;
	}
	
	public boolean isFormulaSupported() {
		return this.isFormulaSupported;
	}
	
	public List<? extends ValueDataType> getValueDataTypes() {
		return this.valueDataTypes;
	}
	
	public List<? extends ConditionalFormat> getConditionalFormats() {
		return this.conditionalFormats;
	}
	
	
	@Override
	public String getDefaultType() {
		return "Dynamic";
	}
	
	@Override
	protected Map<String, Object> checkCustomData(final ColumnConfiguration config) {
		final Map<String, Object> customData = super.checkCustomData(config);
		
		String valueSupplier = (String)customData.get(VALUE_SUPPLIER_KEY);
		if (valueSupplier != null) {
			valueSupplier = valueSupplier.intern();
			customData.put(VALUE_SUPPLIER_KEY, valueSupplier);
		}
		else {
			valueSupplier = SPECIFIED_EXPRESSION;
			customData.put(VALUE_SUPPLIER_KEY, valueSupplier);
			customData.put(EXPRESSION_LANGUAGE_ID_KEY, "javaScript");
		}
		
		if (valueSupplier.equals(PREDEFINED_FORMULA)) {
			CustomDataUtils.checkLong(customData, FORMULA_ID_KEY, null);
		}
		
		ValueDataType valueDataType;
		{	final Object obj = customData.get(VALUE_DATA_TYPE_KEY);
			if (obj instanceof ValueDataType) {
				valueDataType = (ValueDataType)obj;
			} else {
				valueDataType = ValueDataType.getType(this.valueDataTypes, (String)obj);
				customData.put(VALUE_DATA_TYPE_KEY, valueDataType);
			}
		}
		CustomDataUtils.checkTypeDefaultFirst(customData, VALUE_FORMAT_KEY,
				valueDataType.getSupportedFormats() );
		
		CustomDataUtils.checkType(customData, CONDITIONAL_FORMAT_KEY,
				this.conditionalFormats, null );
		
		return customData;
	}
	
	@Override
	public void applyCustomData(final ColumnConfiguration config, final Map<String, Object> customData) {
		try {
			final DynamicColumnLabelProvider<TEntity, TViewerElement> currentDynamicColumn = getDynamicColumn(config.getLabelProvider());
			
			final ValueDataType valueDataType = (ValueDataType)customData.get(VALUE_DATA_TYPE_KEY);
			final DataDescription dataDescription = valueDataType.createDataDescription(
					config.getName(), this.viewerInput.getViewerElementType() );
			
			DataProvider<TEntity, TViewerElement> dataProvider = createDataProvider(dataDescription, customData);
			{	final DataProvider<TEntity, TViewerElement> currentDataProvider = (currentDynamicColumn != null) ? currentDynamicColumn.disconnectDataProvider() : null;
				if (dataProvider.equals(currentDataProvider)) {
					dataProvider = currentDataProvider;
				}
				else {
					if (currentDataProvider != null) {
						currentDataProvider.dispose();
					}
					dataProvider.initialize();
				}
			}
			
			final DynamicColumnLabelProvider<TEntity, TViewerElement> dynamicColumn = new DynamicColumnLabelProvider<>(
					dataDescription, dataProvider,
					createDataFormatSupplier(dataDescription, customData) );
			CellLabelProvider labelProvider = dynamicColumn;
			if (isConditionalFormatSupported(dataDescription)) {
				labelProvider = addConditionalFormatting(dynamicColumn, customData);
			}
			config.setDataDescription(dataDescription);
			config.setLabelProvider(labelProvider);
		}
		finally {
			this.viewerInput.getDataLoader().asyncReload(false);
		}
	}
	
	private DynamicColumnLabelProvider<TEntity, TViewerElement> getDynamicColumn(final CellLabelProvider labelProvider) {
		if (labelProvider instanceof DynamicColumnLabelProvider) {
			return (DynamicColumnLabelProvider<TEntity, TViewerElement>)labelProvider;
		}
		if (labelProvider instanceof DynamicConditionalLabelProvider) {
			return ((DynamicConditionalLabelProvider)labelProvider).getDynamicColumn();
		}
		return null;
	}
	
	private DataProvider<TEntity, TViewerElement> createDataProvider(final DataDescription dataDescription,
			final Map<String, Object> customData) {
		final String valueSupplier = (String)customData.get(VALUE_SUPPLIER_KEY);
		if (valueSupplier == SPECIFIED_EXPRESSION) {
			final ScriptLanguage language = getScriptLanguage((String)customData.get(EXPRESSION_LANGUAGE_ID_KEY));
			final String code = (String)customData.get(EXPRESSION_CODE_KEY);
			if (language != null) {
				return new ExpressionDataProvider<>(dataDescription, this.viewerInput,
						language, (code != null) ? code : "", this.evaluationContext );
			}
			throw new UnsupportedOperationException("valueSupplier= SpecifiedExpression,"
					+ " languageId= " + customData.get(EXPRESSION_LANGUAGE_ID_KEY) );
		}
		else if (this.isFormulaSupported && valueSupplier == PREDEFINED_FORMULA) {
			final Long formulaId = (Long)customData.get(FORMULA_ID_KEY);
			if (formulaId != null) {
				return new FormulaDataProvider<>(dataDescription, this.viewerInput,
						formulaId, this.evaluationContext );
			}
		}
		throw new UnsupportedOperationException("valueSupplier= " + valueSupplier);
	}
	
	private ScriptLanguage getScriptLanguage(final String id) {
		for (final ScriptLanguage language : this.evaluationContext.getScriptLanguages()) {
			if (language.getId().equals(id)) {
				return language;
			}
		}
		return null;
	}
	
	private Supplier<DataFormatter> createDataFormatSupplier(
			final DataDescription dataDescription,
			final Map<String, Object> customData) {
		final ValueFormat format = (ValueFormat)customData.get(VALUE_FORMAT_KEY);
		if (dataDescription == null
				|| format == null || format.getKey().equals("default")) {
			return this.dataFormatSupplier;
		}
		final ValueFormat.FormatConfig formatConfig = format.createConfig(customData);
		return new CustomDataFormatSupplier(formatConfig, this.dataFormatSupplier);
	}
	
	private boolean isConditionalFormatSupported(final DataDescription dataDescription) {
		if (dataDescription == null) {
			return true;
		}
		switch (dataDescription.getDataType()) {
		case Boolean:
		case Integer:
		case Real:
			return true;
		default:
			return false;
		}
	}
	
	private CellLabelProvider addConditionalFormatting(final DynamicColumnLabelProvider<TEntity, TViewerElement> labelProvider,
			final Map<String, Object> customData) {
		final ConditionalFormat format = (ConditionalFormat)customData.get(CONDITIONAL_FORMAT_KEY);
		if (format == null || format == DynamicColumns.NONE_CONDITIONAL_FORMAT) {
			return labelProvider;
		}
		final FormatConfig formatConfig = format.createConfig(customData);
		return new DynamicConditionalLabelProvider<>(labelProvider, formatConfig);
	}
	
	
	@Override
	protected EditCustomColumnDialog createEditDialog(final ColumnConfiguration config, final String type,
			final Shell shell) {
		return new EditCustomColumnDialog(shell, config,
				new ValueTab<>(this),
				new FormattingTab<>(this),
				new ConditionalFormattingTab<>(this) );
	}
	
}
