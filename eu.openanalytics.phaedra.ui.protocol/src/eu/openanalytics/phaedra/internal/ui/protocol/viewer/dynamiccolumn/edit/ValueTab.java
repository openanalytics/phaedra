package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit;

import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.EXPRESSION_CODE_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.EXPRESSION_LANGUAGE_ID_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.FORMULA_ID_KEY;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.PREDEFINED_FORMULA;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.SPECIFIED_EXPRESSION;
import static eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.DynamicColumnCustomData.VALUE_SUPPLIER_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.EditCustomColumnTab;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.ui.protocol.calculation.SelectFormulaDialog;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumnSupport;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptLanguage;


public class ValueTab<TEntity> extends EditCustomColumnTab {
	
	
	private final DynamicColumnSupport<TEntity, ?> columnSupport;
	
	private final List<ScriptLanguage> scriptLanguages;
	private final WritableValue<ScriptLanguage> individualExpressionLanguageValue;
	private final WritableValue<String> individualExpressionCodeValue;
	
	private final boolean isFormulaSupported;
	private final WritableValue<CalculationFormula> predefinedFormulaValue;
	
	final SelectObservableValue<String> supplierSelectionValue = new SelectObservableValue<>(String.class);
	
	private Button individualExpressionControl;
	private Label individualExpressionLanguageLabel;
	private ComboViewer individualExpressionLanguageViewer;
	private Map<String, IContentProposal> individualExpressionVariables;
	private Label individualExpressionCodeLabel;
	private Text individualExpressionCodeControl;
	
	private Button predefinedFormulaControl;
	private Label predefinedFormulaNameLabel;
	private Text predefinedFormulaNameControl;
	private Button predefinedFormulaSelectControl;
	
	
	public ValueTab(final DynamicColumnSupport<TEntity, ?> columnSupport) {
		super("&Value");
		this.columnSupport = columnSupport;
		
		this.scriptLanguages = this.columnSupport.getEvaluationContext().getScriptLanguages();
		this.individualExpressionLanguageValue = new WritableValue<>(null, ScriptLanguage.class);
		this.individualExpressionCodeValue = new WritableValue<>("", String.class);
		this.isFormulaSupported = this.columnSupport.isFormulaSupported();
		this.predefinedFormulaValue = new WritableValue<>(null, CalculationFormula.class);
	}
	
	
	@Override
	protected Composite createContent(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		{	GridLayout layout = createContentGridLayout(2);
			layout.horizontalSpacing += 3;
			composite.setLayout(layout);
		}
		final GridDataFactory detailGridDataFactory = GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.indent(getDetailHorizontalIndent() - 3, 0);
		
		{	final Button button = new Button(composite, SWT.RADIO);
			button.setText("C&ustom script expression:");
			button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
			this.individualExpressionControl = button;
		}
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("&Language:");
			detailGridDataFactory.applyTo(label);
			this.individualExpressionLanguageLabel = label;
			
			final ComboViewer viewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					return ((ScriptLanguage)element).getLabel();
				}
			});
			viewer.setInput(this.scriptLanguages);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			this.individualExpressionLanguageViewer = viewer;
		}
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Code:");
			detailGridDataFactory.copy().align(SWT.FILL, SWT.TOP).applyTo(label);
			this.individualExpressionCodeLabel = label;
			
			final Text text = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = text.getLineHeight() * 4;
			text.setLayoutData(gd);
			this.individualExpressionCodeControl = text;
			
			new ContentAssistCommandAdapter(text, new TextContentAdapter(), this::getProposals, null, null, true);
		}
		
		if (this.isFormulaSupported) {
			{	final Button button = new Button(composite, SWT.RADIO);
				button.setText("&Predefined formula:");
				button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
				this.predefinedFormulaControl = button;
			}
			{	final Label label = new Label(composite, SWT.NONE);
				label.setText("&Formula:");
				detailGridDataFactory.applyTo(label);
				this.predefinedFormulaNameLabel = label;
				
				final Composite formSelection = new Composite(composite, SWT.NONE);
				formSelection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(formSelection);
				
				final Text nameText = new Text(formSelection, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
				nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				this.predefinedFormulaNameControl = nameText;
				
				final Button button = new Button(formSelection, SWT.PUSH);
				button.setText("Select...");
				GridDataFactory.defaultsFor(button).applyTo(button);
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						SelectFormulaDialog dialog = new SelectFormulaDialog(getShell());
						if (dialog.open() == Dialog.OK) {
							predefinedFormulaValue.setValue(dialog.getSelectedFormula());
						}
					}
				});
				this.predefinedFormulaSelectControl = button;
			}
		}
		
		return composite;
	}
	
	
	@Override
	protected void initDataBinding(final DataBindingContext dbc) {
		final IObservableValue<Boolean> individualExpressionSelectValue = WidgetProperties.selection().observe(this.individualExpressionControl);
		this.supplierSelectionValue.addOption(SPECIFIED_EXPRESSION, individualExpressionSelectValue);
		
		bindEnabled(Arrays.asList(
						this.individualExpressionLanguageLabel, this.individualExpressionLanguageViewer.getControl(),
						this.individualExpressionCodeLabel, this.individualExpressionCodeControl),
				individualExpressionSelectValue );
		this.individualExpressionLanguageValue.addValueChangeListener(new IValueChangeListener<ScriptLanguage>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends ScriptLanguage> event) {
				final ScriptLanguage language = event.diff.getNewValue();
				if (language != null) {
					final EditorScriptContext context = new EditorScriptContext(language);
					columnSupport.getEvaluationContext().contributeVariables(context, null);
					individualExpressionVariables = context.getVariables();
				}
			}
		});
		
		dbc.bindValue(
				ViewerProperties.singleSelection().observe(this.individualExpressionLanguageViewer),
				this.individualExpressionLanguageValue );
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(this.individualExpressionCodeControl),
				this.individualExpressionCodeValue );
		
		if (this.isFormulaSupported) {
			final IObservableValue<Boolean> predefinedFormulaSelectValue = WidgetProperties.selection().observe(this.predefinedFormulaControl);
			this.supplierSelectionValue.addOption(PREDEFINED_FORMULA, predefinedFormulaSelectValue);
			
			bindEnabled(Arrays.asList(
							this.predefinedFormulaNameLabel, this.predefinedFormulaNameControl, this.predefinedFormulaSelectControl ),
					predefinedFormulaSelectValue );
			
			dbc.bindValue(
					WidgetProperties.text().observe(this.predefinedFormulaNameControl),
					this.predefinedFormulaValue,
					new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
					new UpdateValueStrategy().setConverter(IConverter.create(CalculationFormula.class, String.class,
							(final Object o) -> (o != null) ? ((CalculationFormula)o).getName() : "" )));
		}
		
		dbc.addValidationStatusProvider(new MultiValidator() {
			@Override
			protected IStatus validate() {
				final String supplier = supplierSelectionValue.getValue();
				final CalculationFormula predefinedFormula = predefinedFormulaValue.getValue();
				final ScriptLanguage language = individualExpressionLanguageValue.getValue();
				final String code = individualExpressionCodeValue.getValue();
				
				if (supplier == PREDEFINED_FORMULA) {
					if (predefinedFormula == null) {
						return ValidationStatus.error("No formula is specified, select a predefined formula.");
					}
					return ValidationStatus.ok();
				}
				else {
					if (language == null) {
						return ValidationStatus.error("The language for the expression is not specified, select a supported language.");
					}
					if (code == null || code.trim().isEmpty()) {
						return ValidationStatus.error("The expression to compute the value is not specified.");
					}
					return ValidationStatus.ok();
				}
			}
		});
	}
	
	@Override
	protected void updateConfig(final Map<String, Object> customData) {
		final String supplier = this.supplierSelectionValue.getValue();
		final CalculationFormula predefinedFormula = this.predefinedFormulaValue.getValue();
		final ScriptLanguage language = this.individualExpressionLanguageValue.getValue();
		final String code = this.individualExpressionCodeValue.getValue();
		customData.put(VALUE_SUPPLIER_KEY, supplier);
		if (supplier == PREDEFINED_FORMULA) {
			customData.put(FORMULA_ID_KEY, (predefinedFormula != null) ? predefinedFormula.getId() : null);
		}
		else if (supplier == SPECIFIED_EXPRESSION) {
			customData.put(EXPRESSION_LANGUAGE_ID_KEY, (language != null) ? language.getId() : null);
			customData.put(EXPRESSION_CODE_KEY, code);
		}
	}
	
	@Override
	protected void updateTargets(final Map<String, Object> customData) {
		String supplier = (String)customData.get(VALUE_SUPPLIER_KEY);
		this.supplierSelectionValue.setValue(supplier);
		if (supplier == SPECIFIED_EXPRESSION) {
			final String languageId = (String)customData.get(EXPRESSION_LANGUAGE_ID_KEY);
			final String code = (String)customData.get(EXPRESSION_CODE_KEY);
			this.individualExpressionLanguageValue.setValue((languageId != null) ? getLanguage(languageId) : null);
			this.individualExpressionCodeValue.setValue((code != null) ? code : "");
		}
		else if (supplier == PREDEFINED_FORMULA) {
			final Long formulaId = (Long)customData.get(FORMULA_ID_KEY);
			this.predefinedFormulaValue.setValue((formulaId != null) ? FormulaService.getInstance().getFormula(formulaId) : null);
		}
	}
	
	
	private ScriptLanguage getLanguage(final String id) {
		for (ScriptLanguage language : this.scriptLanguages) {
			if (language.getId().equals(id)) {
				return language;
			}
		}
		return null;
	}
	
	private IContentProposal[] getProposals(final String contents, final int position) {
		final List<IContentProposal> proposals = new ArrayList<>();
		final Map<String, IContentProposal> variables = this.individualExpressionVariables;
		if (variables != null) {
			proposals.addAll(variables.values());
		}
		proposals.sort(Comparator.comparing(IContentProposal::getContent));
		return proposals.toArray(new IContentProposal[proposals.size()]);
	}
	
}
