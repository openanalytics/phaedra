package eu.openanalytics.phaedra.base.ui.colormethod.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;

public class LookupColorMethodDialog extends BaseColorMethodDialog {

	private RichTableViewer tableViewer;
	private Button addBtn;
	private Button removeBtn;
	private Button clearBtn;
	
	private List<LookupRule> rules;
	private RGB[] availableColors;
	private List<String> availableConditions;
	
	private ColorStore colorStore;
	private LookupColorMethod colorMethod;
	
	public LookupColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		colorStore = new ColorStore();
		
		availableColors = new RGB[10];
		availableColors[0] = new RGB(0, 0, 0);
		availableColors[1] = new RGB(160, 0, 0);
		availableColors[2] = new RGB(250, 125, 0);
		availableColors[3] = new RGB(0, 0, 230);
		availableColors[4] = new RGB(255, 255, 0);
		availableColors[5] = new RGB(0, 255, 255);
		availableColors[6] = new RGB(45, 50, 111);
		availableColors[7] = new RGB(100, 0, 255);
		availableColors[8] = new RGB(255, 0, 100);
		availableColors[9] = new RGB(255, 100, 255);

		availableConditions = new ArrayList<String>();
		availableConditions.add("gt");
		availableConditions.add("lt");
		availableConditions.add("ge");
		availableConditions.add("le");
		availableConditions.add("eq");
		availableConditions.add("ne");
		
		// Copy the original rules.
		colorMethod = (LookupColorMethod)cm;
		List<LookupRule> cmRules = colorMethod.getRules();
		rules = new ArrayList<LookupRule>();
		for (LookupRule cmRule: cmRules) {
			LookupRule rule = new LookupRule(cmRule.getColor(), cmRule.getCondition(), cmRule.getValue());
			rules.add(rule);
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(350, 300);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {

		tableViewer = new RichTableViewer(area, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				StructuredSelection sel = (StructuredSelection)tableViewer.getSelection();
				if (sel.isEmpty()) return;
				LookupRule rule = (LookupRule)sel.getFirstElement();
				new EditRuleDialog(getParentShell(), rule).open();
			}
		});
		GridDataFactory.fillDefaults().grab(true,true).hint(250, 150).applyTo(table);
		
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(configureColumns());

		Composite buttonGroup = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonGroup);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(buttonGroup);
		
		addBtn = new Button(buttonGroup, SWT.PUSH);
		addBtn.setText("Add");
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addRule();
			}
		});
		
		removeBtn = new Button(buttonGroup, SWT.PUSH);
		removeBtn.setText("Remove");
		removeBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeRules();
			}
		});
		
		clearBtn = new Button(buttonGroup, SWT.PUSH);
		clearBtn.setText("Clear All");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clearRules();
			}
		});
		
		tableViewer.setInput(rules);
	}

	@Override
	protected void okPressed() {
		// Transfer new ruleset to the color method.
		colorMethod.getRules().clear();
		colorMethod.getRules().addAll(rules);
		super.okPressed();
	}
	
	@Override
	public boolean close() {
		colorStore.dispose();
		return super.close();
	}

	private void addRule() {
		LookupRule rule = new LookupRule(new RGB(0,255,0), "lt", 100);
		rules.add(rule);
		tableViewer.refresh();
	}
	
	private void removeRules() {
		StructuredSelection sel = (StructuredSelection)tableViewer.getSelection();
		List<LookupRule> rulesToRemove = new ArrayList<LookupRule>();
		for (Iterator<?> it = sel.iterator(); it.hasNext();) {
			LookupRule rule = (LookupRule)it.next();
			rulesToRemove.add(rule);
		}
		rules.removeAll(rulesToRemove);
		tableViewer.refresh();
	}
	
	private void clearRules() {
		rules.clear();
		tableViewer.refresh();
	}
	
	private ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;
		
		config = ColumnConfigFactory.create("Color", ColumnDataType.String, 100);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				LookupRule rule = (LookupRule)cell.getElement();
				cell.setBackground(colorStore.get(rule.getColor()));
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Condition", ColumnDataType.String, 80);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				LookupRule rule = (LookupRule)cell.getElement();
				String lbl = LookupColorMethod.getConditionLabel(rule.getCondition());
				cell.setText(lbl);
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Value", "getValue", ColumnDataType.Numeric, 80);
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
	
	private class EditRuleDialog extends Dialog {

		private ColorSelector colorBtn;
		private Combo conditionCmb;
		private Text valueTxt;
		
		private LookupRule rule;
		
		public EditRuleDialog(Shell parentShell, LookupRule rule) {
			super(parentShell);
			this.rule = rule;
		}
		
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Edit Lookup Rule");
		}

		protected Point getInitialSize() {
			return super.getInitialSize();
		}

		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);

			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(container);

			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Color:");
			
			colorBtn = new ColorSelector(container);
			colorBtn.setColorValue(rule.getColor());
			GridDataFactory.fillDefaults().grab(true,false).applyTo(colorBtn.getButton());
			
			lbl = new Label(container, SWT.NONE);
			lbl.setText("Condition:");
			
			conditionCmb = new Combo(container, SWT.READ_ONLY);
			String[] items = new String[availableConditions.size()];
			for (int i=0; i<items.length; i++) {
				items[i] = LookupColorMethod.getConditionLabel(availableConditions.get(i));
			}
			conditionCmb.setItems(items);
			conditionCmb.select(conditionCmb.indexOf(LookupColorMethod.getConditionLabel(rule.getCondition())));
			GridDataFactory.fillDefaults().grab(true,false).applyTo(conditionCmb);
			
			lbl = new Label(container, SWT.NONE);
			lbl.setText("Value:");
			
			valueTxt = new Text(container, SWT.BORDER);
			valueTxt.setText("" + rule.getValue());
			GridDataFactory.fillDefaults().grab(true,false).applyTo(valueTxt);
			
			setTitle("Edit Lookup Rule");
			return area;
		}
		
		@Override
		protected void okPressed() {
			rule.setColor(colorBtn.getColorValue());
			rule.setCondition(availableConditions.get(conditionCmb.getSelectionIndex()));
			rule.setValue(Double.parseDouble(valueTxt.getText()));
			tableViewer.refresh();
			super.okPressed();
		}
	}
}
