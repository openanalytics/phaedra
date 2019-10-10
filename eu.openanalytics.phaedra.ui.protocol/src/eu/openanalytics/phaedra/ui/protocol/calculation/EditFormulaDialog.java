package eu.openanalytics.phaedra.ui.protocol.calculation;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.dialog.TitleAreaDatabindingDialog;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Language;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;

public class EditFormulaDialog extends TitleAreaDatabindingDialog {

	private Text nameTxt;
	private Text descriptionTxt;
	private Text authorTxt;
	private Text formulaTxt;
	private ComboViewer categoryComboViewer;
	private ComboViewer languageComboViewer;
	private ComboViewer inputTypeComboViewer;
	private ComboViewer scopeComboViewer;
	
	private CalculationFormula formula;
	
	public EditFormulaDialog(Shell parentShell, CalculationFormula formula) {
		super(parentShell);
		
		this.formula = formula;
		setDialogTitle((formula.getId() == 0) ? "Create New Formula" : "Edit Formula");
		setDialogMessage("Edit the properties of the formula below.");
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600, 600);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(main);

		Group group = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(group);
		
		new Label(group, SWT.NONE).setText("Name:");
		nameTxt = new Text(group, SWT.BORDER);
		nameTxt.setFocus();
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameTxt);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionTxt = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		descriptionTxt.setTextLimit(2000);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 50).applyTo(descriptionTxt);
		
		new Label(group, SWT.NONE).setText("Category:");
		Combo categoryCmb = new Combo(group, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(categoryCmb);
		categoryComboViewer = createComboViewer(categoryCmb, FormulaService.getInstance().getFormulaCategories(), el -> el.toString());
		
		new Label(group, SWT.NONE).setText("Author:");
		authorTxt = new Text(group, SWT.BORDER);
		authorTxt.setEnabled(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(authorTxt);
		
		group = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(group);
		
		new Label(group, SWT.NONE).setText("Language:");
		Combo languageCmb = new Combo(group, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(languageCmb);
		languageComboViewer = createComboViewer(languageCmb, FormulaService.getInstance().getLanguages(), el -> ((Language) el).getLabel());
		
		new Label(group, SWT.NONE).setText("Evaluate:");
		Combo scopeCmb = new Combo(group, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scopeCmb);
		scopeComboViewer = createComboViewer(scopeCmb, Scope.values(), el -> ((Scope) el).getLabel());
		
		new Label(group, SWT.NONE).setText("Work with:");
		Combo inputTypeCmb = new Combo(group, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputTypeCmb);
		inputTypeComboViewer = createComboViewer(inputTypeCmb, InputType.values(), el -> ((InputType) el).getLabel());
		
		lbl = new Label(group, SWT.NONE);
		lbl.setText("Formula:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		formulaTxt = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(formulaTxt);
		
		return main;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initDatabinding(DataBindingContext dbc) {
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(nameTxt),
				PojoProperties.value("name", String.class).observe(formula));
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(descriptionTxt),
				PojoProperties.value("description", String.class).observe(formula));
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(authorTxt),
				PojoProperties.value("author", String.class).observe(formula));
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(formulaTxt),
				PojoProperties.value("formula", String.class).observe(formula));
		dbc.bindValue(
				WidgetProperties.selection().observe(categoryComboViewer.getCombo()),
				PojoProperties.value("category", String.class).observe(formula));
		dbc.bindValue(
				WidgetProperties.selection().observe(languageComboViewer.getCombo()),
				createBeanFieldMapper(formula,
						() -> FormulaService.getInstance().getLanguage(formula.getLanguage()).getLabel(),
						label -> Arrays.stream(FormulaService.getInstance().getLanguages())
									.filter(l -> l.getLabel().equals(label)).findAny()
									.ifPresent(l -> formula.setLanguage(l.getId()))
				));
		dbc.bindValue(
				WidgetProperties.selection().observe(inputTypeComboViewer.getCombo()),
				createBeanFieldMapper(formula,
						() -> InputType.get(formula.getInputType()).getLabel(),
						label -> formula.setInputType(InputType.getByLabel((String) label).getCode())
				));
		dbc.bindValue(
				WidgetProperties.selection().observe(scopeComboViewer.getCombo()),
				createBeanFieldMapper(formula,
						() -> Scope.get(formula.getScope()).getLabel(),
						label -> formula.setScope(Scope.getByLabel((String) label).getCode())
				));
	}

	private ComboViewer createComboViewer(Combo combo, Object[] input, Function<Object, String> labelProvider) {
		ComboViewer cmbViewer = new ComboViewer(combo);
		cmbViewer.setContentProvider(new ArrayContentProvider());
		cmbViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return labelProvider.apply(element);
			}
		});
		cmbViewer.setInput(input);
		return cmbViewer;
	}
	
	@SuppressWarnings("unchecked")
	private IObservableValue<?> createBeanFieldMapper(CalculationFormula formula, Supplier<Object> getter, Consumer<Object> setter) {
		return PojoProperties.value("field").observe(new FieldMapper(getter, setter));
	}
	
	private static class FieldMapper {
		
		private Supplier<Object> getter;
		private Consumer<Object> setter;

		public FieldMapper(Supplier<Object> getter, Consumer<Object> setter) {
			this.getter = getter;
			this.setter = setter;
		}

		@SuppressWarnings("unused")
		public Object getField() {
			return getter.get();
		}
		
		@SuppressWarnings("unused")
		public void setField(Object value) {
			setter.accept(value);
		}
	}
}
