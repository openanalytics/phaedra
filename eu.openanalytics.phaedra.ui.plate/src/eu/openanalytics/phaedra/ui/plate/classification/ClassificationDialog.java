package eu.openanalytics.phaedra.ui.plate.classification;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchActionConstants;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.ClassificationProvider;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class ClassificationDialog<T> extends TitleAreaDialog {

	private Combo providerCombo;
	private CheckboxTableViewer classTableViewer;
	private RichTableViewer itemTableViewer;

	private ColorStore colorStore;
	private BaseClassificationSupport<T> classificationSupport;
	
	protected ClassificationDialog(Shell parentShell, BaseClassificationSupport<T> classificationSupport) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE | getDefaultOrientation() | SWT.MODELESS);
		this.colorStore = new ColorStore();
		this.classificationSupport = classificationSupport;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setSize(600, 550);
		shell.setText("Edit Classification");
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.NEXT_ID, "Apply Classification", true);
		createButton(parent, IDialogConstants.OK_ID, "Finish", false).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	public int open() {
		return super.open();
	}

	protected Control createDialogArea(Composite parent) {

		Composite area = (Composite) super.createDialogArea(parent);

		Composite container = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).equalWidth(false).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		Label featureLabel = new Label(container, SWT.None);
		featureLabel.setText("Classification feature:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(false, false).applyTo(featureLabel);

		providerCombo = new Combo(container, SWT.READ_ONLY); 
		GridDataFactory.fillDefaults().grab(true, false).applyTo(providerCombo);
		providerCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = providerCombo.getSelectionIndex();
				ClassificationProvider[] providers = (ClassificationProvider[])providerCombo.getData();
				classificationSupport.setCurrentClassificationProvider(providers[index]);
				reloadClassTable();
			}
		});
		
		initProviderCombo();
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Class to apply:");
		GridDataFactory.fillDefaults().applyTo(lbl);

		SelectionListener noSelectionAllowed = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Table rt = (Table) e.getSource();
				rt.deselectAll();
			}
		};

		classTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		classTableViewer.getTable().setLinesVisible(true);
		classTableViewer.getTable().setHeaderVisible(true);
		classTableViewer.getTable().addSelectionListener(noSelectionAllowed);
		classTableViewer.setContentProvider(new ArrayContentProvider());
		classTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				classTableViewer.setCheckedElements(new Object[] { event.getElement() });
			}
		});
		GridDataFactory.fillDefaults().hint(100, 90).grab(true, false).applyTo(classTableViewer.getTable());

		lbl = new Label(container, SWT.None);
		lbl.setText("Affected items:");
		GridDataFactory.fillDefaults().applyTo(lbl);

		itemTableViewer = new RichTableViewer(container, SWT.BORDER);
		itemTableViewer.getTable().setLinesVisible(true);
		itemTableViewer.getTable().setHeaderVisible(true);
		itemTableViewer.getTable().addSelectionListener(noSelectionAllowed);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(itemTableViewer.getTable());
		createContextMenu();

		Composite regionComposite = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(2,1).grab(true,false).applyTo(regionComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(regionComposite);
		
		// Data initialization.
		
		createClassTableColumns();
		itemTableViewer.applyColumnConfig(classificationSupport.createItemTableColumns());
		
		classTableViewer.setContentProvider(new ArrayContentProvider());
		itemTableViewer.setContentProvider(new ArrayContentProvider());
		
		reloadClassTable();
		reloadItemTable();
		
		setTitle("Edit Classification");
		setMessage("Update the selected items by selecting a classification feature and a class.\nThis may trigger a plate recalculation.");
		return area;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.OK_ID: 
			if (classificationSupport.doSave()) super.okPressed();
			break;
		case IDialogConstants.CANCEL_ID:
			boolean hasChanges = classificationSupport.getNumBatches() > 0;
			boolean confirm = true;
			if (hasChanges) confirm = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Cancel classification?", 
					"You have made changes to the classification of this plate. Do you wish to discard these changes?");
			if (confirm) super.cancelPressed();
			break;
		case IDialogConstants.NEXT_ID:
			try {
				Object[] el = classTableViewer.getCheckedElements();
				FeatureClass fClass = (el == null || el.length == 0) ? null : (FeatureClass)el[0];
				classificationSupport.getCurrentBatch().setFeatureClass(fClass);
				classificationSupport.addBatch();
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			} catch (IllegalArgumentException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot add classification", e.getMessage());
			}
		}
	}
	
	@Override
	public boolean close() {
		colorStore.dispose();
		return super.close();
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(itemTableViewer.getControl());
		itemTableViewer.getControl().setMenu(menu);
	}

	private void initProviderCombo() {
		ClassificationProvider[] providers = classificationSupport.getClassificationProviders();
		providerCombo.setData(providers);
		for (int i=0; i<providers.length; i++) providerCombo.add(providers[i].getName());
		if (classificationSupport.getCurrentClassificationProvider() != null) {
			int index = CollectionUtils.find(providers, classificationSupport.getCurrentClassificationProvider());
			providerCombo.select(index);
		}
	}
	
	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		itemTableViewer.contributeConfigButton(manager);
	}

	private void reloadClassTable() {
		List<FeatureClass> featureClassList = new ArrayList<>();
		ClassificationProvider provider = classificationSupport.getCurrentClassificationProvider();
		if (provider != null) {
			featureClassList= provider.getFeatureClasses();
		}
		classTableViewer.setInput(featureClassList);
		classTableViewer.setAllChecked(false);
//		FeatureClass currentClass = classificationSupport.getCurrentClass();
//		if (currentClass != null) {
//				classTableViewer.setChecked(currentClass, true);
//		}
	}
	
	/* package */ void reloadItemTable() {
		if (itemTableViewer.getControl().isDisposed()) return;
		T[] items = classificationSupport.getCurrentBatch().getItems();
		itemTableViewer.setInput(items);
	}
	
	private void createClassTableColumns() {
		TableViewerColumn column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(120);
		column.getColumn().setText("Class");
		column.getColumn().setAlignment(SWT.CENTER);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((FeatureClass) element).getLabel();
			}
		});

		column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(60);
		column.getColumn().setText("Symbol");
		column.getColumn().setAlignment(SWT.CENTER);
		column.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void paint(Event event, Object element) {
				String shape = ((FeatureClass) element).getSymbol();
				if(shape != null && !shape.equals("")){
					int color = ((FeatureClass) element).getRgbColor();
					RGB rgb = ColorUtils.hexToRgb(color);
					GC gc = event.gc;
					gc.setAntialias(SWT.ON);
					PlotShape ps = PlotShape.valueOf(shape);
					gc.setForeground(colorStore.get(rgb));
					gc.setBackground(colorStore.get(rgb));
					ps.drawShape(gc, event.x+ gc.getClipping().width/2, event.y+ event.height/2, 5, true);
				}
			}
			@Override
			protected void measure(Event event, Object element) {
				// Do nothing
			}
		});

		column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(120);
		column.getColumn().setText("Description");
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((FeatureClass) element).getDescription();
			}
		});

		column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(50);
		column.getColumn().setText("Count");
		column.getColumn().setAlignment(SWT.CENTER);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				String calculation = new String();
				calculation = classificationSupport.calculateCount((FeatureClass)element, false);
				return calculation;
			}
		});

		column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(70);
		column.getColumn().setText("Percent");
		column.getColumn().setAlignment(SWT.CENTER);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				String calculation = new String();
				calculation = classificationSupport.calculateCount((FeatureClass)element, true);
				return calculation;
			}
		});
	}
}