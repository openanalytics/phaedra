package eu.openanalytics.phaedra.calculation.jep;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.scripting.jep.JEPConstant;
import eu.openanalytics.phaedra.base.scripting.jep.JEPFunction;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

//TODO Extract protocol dependencies and move to the jep bundle.
public class JEPFormulaDialog extends Dialog {

	private ProtocolClass pClass;

	private Text formulaText;
	private String formula;
	private Listener tooltipListener;

	private int totalTables;

	public JEPFormulaDialog(Shell parentShell, ProtocolClass pClass) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.RESIZE);

		this.pClass = pClass;
		this.totalTables = 0;
	}

	@Override
	public int open() {
		if (formula != null && formulaText != null && !formulaText.isDisposed()) {
			formulaText.setText(formula);
		}
		return super.open();
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(850, 500);
		newShell.setText("Enter JEP Expression...");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Save", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		formula = formulaText.getText();
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);

		createTables(container);

		GridLayoutFactory.fillDefaults().numColumns(totalTables).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(totalTables, 1).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(subContainer);

		Label lbl = new Label(subContainer, SWT.NONE);
		lbl.setText("Expression:");

		formulaText = new Text(subContainer, SWT.BORDER | SWT.WRAP | SWT.SINGLE);
		if (formula == null) formula = "";
		formulaText.setText(formula);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(formulaText);

		return parent;
	}

	protected void createExpression(Composite subContainer) {
		new Label(subContainer, SWT.NONE).setText("Expression:");

		formulaText = new Text(subContainer, SWT.BORDER | SWT.WRAP | SWT.SINGLE);
		if (formula == null) formula = "";
		formulaText.setText(formula);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(formulaText);
	}

	protected void createTables(Composite container) {
		createWellFeatureTable(container);
		createSubWellFeatureTable(container);
		createFeatureNormalizationsTable(container);
		createWellPropertiesTable(container);
		createFunctionTable(container);
		createCurvePropertiesTable(container);
		createConstantTable(container);
	}

	protected Table createTable(Composite container, String title, int minWidth, Function<String, String> textToInsert) {
		Composite tableComposite = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).minSize(minWidth, 200).applyTo(tableComposite);
		GridLayoutFactory.fillDefaults().applyTo(tableComposite);

		Label lbl = new Label(tableComposite, SWT.NONE);
		lbl.setText(title);

		Table table = new Table(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION );
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		totalTables++;

		if (textToInsert != null) addSelectionListener(table, textToInsert);
		
		return table;
	}

	protected Text getFormulaText() {
		return formulaText;
	}

	private void createWellFeatureTable(Composite container) {
		final Table table = createTable(container, "Well Features:", 100, s -> "#" + s + "#");
		List<Feature> features = ProtocolUtils.getFeatures(pClass);
		for (Feature feature : features) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, feature.getDisplayName());
			item.setData("info", feature.getName());
		}
		table.addListener(SWT.MouseMove, createTooltipListener());
	}

	private void createSubWellFeatureTable(Composite container) {
		final Table table = createTable(container, "Subwell Features:", 100, s -> "$" + s + "$");
		List<SubWellFeature> subWellFeatures = ProtocolUtils.getSubWellFeatures(pClass);
		for (SubWellFeature feature : subWellFeatures) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, feature.getDisplayName());
			item.setData("info", feature.getName());
		}
		table.addListener(SWT.MouseMove, createTooltipListener());
	}

	private void createFeatureNormalizationsTable(Composite container) {
		final Table table = createTable(container, "Normalizations:", 110, s -> "#featureName->" + s + "#");
		String[] normalizations = NormalizationService.getInstance().getNormalizations();
		for (int i = 0 ; i < normalizations.length; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, normalizations[i]);
		}
	}

	private void createWellPropertiesTable(Composite container) {
		final Table table = createTable(container, "Well Properties:", 100, s -> "@" + s + "@");
		WellProperty[] properties = WellProperty.values();
		for (WellProperty prop: properties) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, prop.getLabel());
		}
	}

	private void createCurvePropertiesTable(Composite container) {
		final Table table = createTable(container, "Curve Properties:", 100, s -> "%featureName->" + s + "%");
		//TODO Cannot make dependency to model.curve.
		String[] properties = { "pIC50", "pLAC", "r2", "Weights", "Hill" };
		for (int i = 0 ; i < properties.length; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, properties[i]);
		}
	}
	
	private void createConstantTable(Composite container) {
		final Table table = createTable(container, "Constants:", 70, s -> s);
		for (int i = 0; i < JEPConstant.values().length; i++) {
			JEPConstant c = JEPConstant.values()[i];
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, c.toString());
			item.setData("info", c.getDescription());
		}
		table.addListener(SWT.MouseMove, createTooltipListener());
	}

	private void createFunctionTable(Composite container) {
		final Table tableFunction = createTable(container, "Functions:", 100, null);
		JEPFunction[] functions = JEPFunction.values();
		Arrays.sort(functions, (f1, f2) -> f1.getFunctionName().compareTo(f2.getFunctionName()));
		for (int i = 0; i < functions.length; i++) {
			TableItem item = new TableItem(tableFunction, SWT.NONE);
			item.setText(0, functions[i].getFunctionFullName());
			item.setData("info", functions[i].getDescription());
			item.setData("function", functions[i].toString());
		}
		tableFunction.addListener(SWT.Selection, e -> {
			// Writes the selected function commands to the formulaText box
			if (tableFunction.getSelection().length == 0) return;
			JEPFunction selected = JEPFunction.valueOf(tableFunction.getSelection()[0].getData("function").toString());
			if (selected != null) {
				String selectedString = formulaText.getSelectionText();
				StringBuilder newStr = new StringBuilder(selected.getFunctionName());
				newStr.append('(');
				for (int i = 0; i < selected.getNrArgs(); i++) {
					newStr.append(i > 0 ? ", " : "");
					if (i == 0 && selectedString != null) newStr.append(selectedString);
				}
				newStr.append(')');
				formulaText.insert(newStr.toString());
				if (selected.getNrArgs() > 0 && selectedString == "") {
					int caretPos = formulaText.getCaretPosition();
					formulaText.setSelection(1 + formulaText.getText().indexOf('(', caretPos - newStr.toString().length()));
				}
				tableFunction.deselectAll();
				formulaText.forceFocus();
			}
		});
		tableFunction.addListener(SWT.MouseMove, createTooltipListener());
	}

	private void addSelectionListener(Table table, Function<String, String> textToInsert) {
		table.addListener(SWT.Selection, e -> {
			if (table.getSelection().length == 0) return;
			String selected = table.getSelection()[0].getText();
			if (selected == null || selected.isEmpty()) return;
			formulaText.insert(textToInsert.apply(selected));
			table.deselectAll();
			formulaText.forceFocus();
		});
	}
	
	private Listener createTooltipListener() {
		if (tooltipListener == null) {
			tooltipListener = e -> {
				// Show the corresponding tool tip for each item on mouse over.
				if (e.widget instanceof Table) {
					Table table = (Table) e.widget;
					Point point = new Point(e.x, e.y);
					TableItem item = table.getItem(point);
					if (item != null) {
						table.setToolTipText(item.getData("info").toString());
					} else {
						table.setToolTipText("");
					}
				}
			};
		}
		return tooltipListener;
	}

}