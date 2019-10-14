package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRule;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRuleset;

public class HitCallRulesetEditor extends Composite {

	private TableViewer rulesTableViewer;
	private Button showInUIBtn;
	private ColorSelector colorSelector;
	private TableCombo styleCombo;
	
	private HitCallRuleset ruleset;
	private WritableList<HitCallRule> ruleList;
	
	public HitCallRulesetEditor(Composite parent, int style) {
		this(parent, style, null);
	}
	
	public HitCallRulesetEditor(Composite parent, int style, Listener listener) {
		super(parent, style);
		GridLayoutFactory.fillDefaults().applyTo(this);
		final Listener dirtyListener = (listener == null) ? e -> {} : listener;

		Group group = new Group(this, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(group);
		
		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Rules:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		rulesTableViewer = new TableViewer(group, SWT.FULL_SELECTION | SWT.BORDER);
		rulesTableViewer.getTable().setLinesVisible(true);
		rulesTableViewer.getTable().setHeaderVisible(true);
		rulesTableViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(rulesTableViewer.getControl());
		configureColumns(dirtyListener);
		
		new Label(group, SWT.NONE);
		Composite btnComposite = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnComposite);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(btnComposite);
		
		lbl = new Label(btnComposite, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("tag_blue_add.png"));

		Link link = new Link(btnComposite, SWT.NONE);
		link.setText("<a>Add rule</a>");
		link.addListener(SWT.Selection, e -> {
			if (ruleset == null || ruleList == null) return;
			HitCallService.getInstance().createRule(ruleset);
			rulesTableViewer.refresh();
			dirtyListener.handleEvent(null);
		});
		
		lbl = new Label(btnComposite, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("tag_blue_delete.png"));

		link = new Link(btnComposite, SWT.NONE);
		link.setText("<a>Remove rule</a>");
		link.addListener(SWT.Selection, e -> {
			if (ruleset == null || ruleList == null) return;
			HitCallRule rule = SelectionUtils.getFirstObject(rulesTableViewer.getSelection(), HitCallRule.class);
			ruleList.remove(rule);
			rulesTableViewer.refresh();
			dirtyListener.handleEvent(null);
		});
		
		group = new Group(this, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(group);
		
		showInUIBtn = new Button(group, SWT.CHECK);
		showInUIBtn.setText("Show on plates");
		showInUIBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ruleset != null) ruleset.setShowInUI(showInUIBtn.getSelection());
				dirtyListener.handleEvent(null);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(showInUIBtn);
		
		
		new Label(group, SWT.NONE).setText("Color:");
		colorSelector = new ColorSelector(group);
		colorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (ruleset != null && event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					ruleset.setColor(ColorUtils.rgbToHex(colorSelector.getColorValue()));
				}
				dirtyListener.handleEvent(null);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorSelector.getButton());
		
		new Label(group, SWT.NONE).setText("Style:");
		styleCombo = new TableCombo(group, SWT.BORDER | SWT.READ_ONLY);
		styleCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ruleset != null) ruleset.setStyle(styleCombo.getSelectionIndex());
				dirtyListener.handleEvent(null);
			}
		});
		for (HitCallRenderStyle s: HitCallRenderStyle.values()) {
			TableItem item = new TableItem(styleCombo.getTable(), SWT.NONE);
			item.setText(s.name());
			item.setImage(s.getImage());
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(styleCombo);
	}
	
	public void setInput(HitCallRuleset ruleset) {
		this.ruleset = ruleset;
		if (ruleset == null) {
			this.ruleList = null;
			rulesTableViewer.setInput(new WritableList<>());
			showInUIBtn.setSelection(false);
			colorSelector.setColorValue(new RGB(0,0,0));
			styleCombo.select(0);
		} else {
			this.ruleList = new WritableList<>(ruleset.getRules(), HitCallRule.class);
			rulesTableViewer.setInput(ruleList);
			showInUIBtn.setSelection(ruleset.isShowInUI());
			colorSelector.setColorValue(ColorUtils.hexToRgb(ruleset.getColor()));
			styleCombo.select(ruleset.getStyle());
		}
	}

	private void configureColumns(Listener dirtyListener) {
		TableViewerColumn tvc = new TableViewerColumn(rulesTableViewer, SWT.NONE);
		tvc.getColumn().setText("Name");
		tvc.getColumn().setWidth(200);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((HitCallRule) element).getName();
			}
		});
		tvc.setEditingSupport(new NameEditingSupport(rulesTableViewer, dirtyListener));
		
		tvc = new TableViewerColumn(rulesTableViewer, SWT.NONE);
		tvc.getColumn().setText("Formula");
		tvc.getColumn().setWidth(200);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				CalculationFormula formula = ((HitCallRule) element).getFormula();
				if (formula == null) return "";
				return formula.getName();
			}
		});
		tvc.setEditingSupport(new FormulaEditingSupport(rulesTableViewer, dirtyListener));
		
		tvc = new TableViewerColumn(rulesTableViewer, SWT.NONE);
		tvc.getColumn().setText("Threshold");
		tvc.getColumn().setWidth(75);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return String.valueOf(((HitCallRule) element).getThreshold());
			}
		});
		tvc.setEditingSupport(new ThresholdEditingSupport(rulesTableViewer, dirtyListener));
	}
	
	private static abstract class BaseEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final Listener dirtyListener;

		public BaseEditingSupport(TableViewer viewer, Listener dirtyListener) {
			super(viewer);
			this.viewer = viewer;
			this.dirtyListener = dirtyListener;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (dirtyListener != null) dirtyListener.handleEvent(null);
			viewer.update(element, null);
		}
	}
	
	private static class NameEditingSupport extends BaseEditingSupport {

		public NameEditingSupport(TableViewer viewer, Listener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer) getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			return ((HitCallRule) element).getName();
		}

		@Override
		protected void setValue(Object element, Object value) {
			((HitCallRule) element).setName(String.valueOf(value));
			super.setValue(element, value);
		}
	}
	
	private static class FormulaEditingSupport extends BaseEditingSupport {

		public FormulaEditingSupport(TableViewer viewer, Listener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new FormulaCellEditor(((TableViewer) getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			return ((HitCallRule) element).getFormula();
		}

		@Override
		protected void setValue(Object element, Object value) {
			((HitCallRule) element).setFormula((CalculationFormula) value);
			super.setValue(element, value);
		}
	}
	
	private static class FormulaCellEditor extends DialogCellEditor {
		
		public FormulaCellEditor(Composite parent) {
			super(parent);
		}
		
		@Override
		protected Object openDialogBox(Control cellEditorWindow) {
			SelectFormulaDialog dialog = new SelectFormulaDialog(Display.getDefault().getActiveShell());
			if (dialog.open() == Dialog.OK) return dialog.getSelectedFormula();
			return null;
		}
	}
	
	private static class ThresholdEditingSupport extends BaseEditingSupport {

		public ThresholdEditingSupport(TableViewer viewer, Listener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer) getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			return String.valueOf(((HitCallRule) element).getThreshold());
		}

		@Override
		protected void setValue(Object element, Object value) {
			try {
				double threshold = Double.parseDouble(String.valueOf(value));
				((HitCallRule) element).setThreshold(threshold);
				super.setValue(element, value);
			} catch (NumberFormatException e) {}
		}
	}
}
