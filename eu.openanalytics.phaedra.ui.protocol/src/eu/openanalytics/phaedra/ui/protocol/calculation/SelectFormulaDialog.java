package eu.openanalytics.phaedra.ui.protocol.calculation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;

// Java.type("eu.openanalytics.phaedra.ui.protocol.calculation.SelectFormulaDialog").openNew()
public class SelectFormulaDialog extends TitleAreaDialog {

	private ComboViewer categoryComboViewer;
	private RichTableViewer tableViewer;
	
	private CalculationFormula selectedFormula;
	
	public SelectFormulaDialog(Shell parentShell) {
		super(parentShell);
	}

	public static CalculationFormula openNew() {
		SelectFormulaDialog dialog = new SelectFormulaDialog(Display.getDefault().getActiveShell());
		if (dialog.open() == Dialog.OK) return dialog.getSelectedFormula();
		return null;
	}
	
	public CalculationFormula getSelectedFormula() {
		return selectedFormula;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Formula");
		newShell.setSize(600, 400);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
		
		new Label(main, SWT.NONE).setText("Category:");
		Combo categoryCmb = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(categoryCmb);
		
		categoryComboViewer = new ComboViewer(categoryCmb);
		categoryComboViewer.setContentProvider(new ArrayContentProvider());
		categoryComboViewer.setLabelProvider(new LabelProvider());
		categoryComboViewer.addSelectionChangedListener(e -> resetTableInput());
		
		new Label(main, SWT.NONE);
		tableViewer = new RichTableViewer(main, SWT.BORDER | SWT.SINGLE);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.addSelectionChangedListener(e -> selectedFormula = SelectionUtils.getFirstObject(e.getSelection(), CalculationFormula.class));
		tableViewer.addDoubleClickListener(e -> {
			selectedFormula = SelectionUtils.getFirstObject(e.getSelection(), CalculationFormula.class);
			okPressed();
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
		configureColumns();
		
		MenuManager menuMgr = new MenuManager(null);
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		
		setTitle("Select Formula");
		setMessage("Select a calculation formula from the list below.");
		
		categoryComboViewer.setInput(FormulaService.getInstance().getFormulaCategories(true));
		categoryComboViewer.setSelection(new StructuredSelection("All"));
		
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button newBtn = createButton(parent, IDialogConstants.NEXT_ID, "New...", true);
		newBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreateFormulaHandler.execute();
				resetTableInput();
			}
		});
		super.createButtonsForButtonBar(parent);
	}

	private void configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<>();
		
		configs.add(ColumnConfigFactory.create("ID", "getId", ColumnDataType.Numeric, 50));
		configs.add(ColumnConfigFactory.create("Name", "getName", ColumnDataType.String, 150));
		configs.add(ColumnConfigFactory.create("Category", "getCategory", ColumnDataType.String, 150));
		configs.add(ColumnConfigFactory.create("Language",
				f -> FormulaService.getInstance().getLanguage(((CalculationFormula) f).getLanguage()).getLabel(), ColumnDataType.String, 75));
		configs.add(ColumnConfigFactory.create("Author", "getAuthor", ColumnDataType.String, 75));
		
		tableViewer.applyColumnConfig(configs);
	}
	
	private void fillContextMenu(IMenuManager menuMgr) {
		CalculationFormula selectedFormula = SelectionUtils.getFirstObject(tableViewer.getSelection(), CalculationFormula.class);
		boolean canEdit = FormulaService.getInstance().canEditFormula(selectedFormula);
		if (!canEdit) return;
		
		menuMgr.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setText("Edit...");
				item.addListener(SWT.Selection, e -> {
					EditFormulaHandler.execute(selectedFormula);
					resetTableInput();
				});
				item = new MenuItem(menu, SWT.PUSH);
				item.setText("Delete");
				item.addListener(SWT.Selection, e -> {
					DeleteFormulaHandler.execute(selectedFormula);
					resetTableInput();
				});
			}
			@Override
			public boolean isDynamic() {
				return true;
			}
		});
	}
	
	private void resetTableInput() {
		tableViewer.setInput(FormulaService.getInstance().getFormulae(categoryComboViewer.getCombo().getText()));
	}
}
