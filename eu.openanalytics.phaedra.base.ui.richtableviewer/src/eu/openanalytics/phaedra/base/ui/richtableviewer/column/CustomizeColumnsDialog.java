package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;

public class CustomizeColumnsDialog extends TitleAreaDialog {

	protected Composite compositeRight;
	
	protected CheckboxTableViewer configTableViewer;
	protected Table configTable;
	
	protected RichTableViewer tableviewer;

	public CustomizeColumnsDialog(Shell parentShell, RichTableViewer tableviewer) {
		super(parentShell);
		this.tableviewer = tableviewer;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().applyTo(container);
		
		TabFolder tabFolder = new TabFolder(container, SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).indent(2,0).applyTo(tabFolder);
		
		Composite defaultContainer = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(defaultContainer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(defaultContainer);
		TabItem defaultTab = new TabItem(tabFolder, SWT.NONE);
		defaultTab.setText("Default");
		defaultTab.setControl(defaultContainer);

		final Composite compositeLeft = new Composite(defaultContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(compositeLeft);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(compositeLeft);

		final Text textSearch = new Text(compositeLeft, SWT.BORDER | SWT.SEARCH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true,false).applyTo(textSearch);
		
		textSearch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String text = textSearch.getText();
				if (text != null && !text.isEmpty()) {
					List<TableColumn> columns = new ArrayList<>();
					for (TableColumn column : tableviewer.getTable().getColumns())
						if (column.getText().toLowerCase().contains(text.toLowerCase())) columns.add(column);
					setConfigTableViewerInput(columns.toArray(new TableColumn[columns.size()]));
				} else {
					setConfigTableViewerInput(tableviewer.getTable().getColumns());
				}
			}
		});

		configTableViewer = CheckboxTableViewer.newCheckList(compositeLeft, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		configTable = configTableViewer.getTable();
		configTable.setLinesVisible(true);
		configTable.setHeaderVisible(true);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(configTable);

		compositeRight = new Composite(defaultContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(compositeRight);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeRight);		

		final Button buttonSearch = new Button(compositeRight, SWT.NONE);
		buttonSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonSearch.setText("Clear search");
		buttonSearch.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				textSearch.setText("");
				setConfigTableViewerInput(tableviewer.getTable().getColumns());
			}
		});

		final Button button = new Button(compositeRight, SWT.NONE);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setText("Select All");
		button.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				configTableViewer.setAllChecked(true);
				TableColumn[] columns = tableviewer.getTable().getColumns();
				tableviewer.getTable().setRedraw(false);
				for (TableColumn column : columns) {
					toggleColumnVisible(column, true);
				}
				tableviewer.getTable().setRedraw(true);
			}
		});

		final Button button_1 = new Button(compositeRight, SWT.NONE);
		button_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button_1.setText("Select None");
		button_1.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				configTableViewer.setAllChecked(false);
				TableColumn[] columns = tableviewer.getTable().getColumns();
				tableviewer.getTable().setRedraw(false);
				for (TableColumn column : columns) {
					toggleColumnVisible(column, false);
				}
				tableviewer.getTable().setRedraw(true);
			}
		});

		// create properties

		final Composite compositeProperties = new Composite(defaultContainer, SWT.NONE);		
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(compositeProperties);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,  false).applyTo(compositeProperties);

		loadColumns();
		addCheckboxListener();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IConfigurableColumnType.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IConfigurableColumnType.ATTR_CLASS);
				if (o instanceof IConfigurableColumnType) {
					IConfigurableColumnType settingsTab = (IConfigurableColumnType) o;
					if (tableviewer.hasColumnDataType(settingsTab.getColumnDataType())) {
						Composite tabContainer = new Composite(tabFolder, SWT.NONE);
						GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(tabContainer);
						GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);
						TabItem tab = new TabItem(tabFolder, SWT.NONE);
						tab.setText(settingsTab.getName());
						tab.setControl(tabContainer);
						settingsTab.fillConfigArea(tabContainer, tableviewer);
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}

		setTitle("Configure Columns");
		setMessage("You can change the appearance of the table columns shown below.");
		
		return container;
	}
	
	protected void updateSelectedColumns() {
//		TableColumn[] columns = tableviewer.getTable().getColumns();
//		tableviewer.getTable().setRedraw(false);
//		for (TableColumn column : columns) {
//			toggleColumnVisible(column, true);
//		}
//		tableviewer.getTable().setRedraw(true);
	}

	private void addCheckboxListener() {
		configTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				TableColumn column = (TableColumn) event.getElement();
				if (!event.getChecked()) {
					toggleColumnVisible(column, false);
				} else {
					toggleColumnVisible(column, true);
				}
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void loadColumns() {
		TableViewerColumn column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(150);
		column.getColumn().setText("Column");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((TableColumn) element).getText();
			}
		});
		Comparator<?> sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				return o1.getText().toLowerCase().compareTo(o2.getText().toLowerCase());
			}
		};
		new ColumnViewerSorter(column.getViewer(), column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(80);
		column.getColumn().setText("Data Type");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				TableColumn col = (TableColumn) element;
				ColumnConfiguration cfg = (ColumnConfiguration)col.getData();
				ColumnDataType dataType = cfg.getDataType();
				if (dataType == null) return "";
				return dataType.toString();
			}
		});
		sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				String dt1 = ((ColumnConfiguration) o1.getData()).getDataType() + "";
				String dt2 = ((ColumnConfiguration) o2.getData()).getDataType() + "";
				return dt1.toLowerCase().compareTo(dt2.toLowerCase());
			}
		};
		new ColumnViewerSorter(column.getViewer(), column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(250);
		column.getColumn().setText("Description");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				TableColumn col = (TableColumn) element;
				return col.getToolTipText();
			}
		});
		sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				return o1.getToolTipText().toLowerCase().compareTo(o2.getToolTipText().toLowerCase());
			}
		};
		new ColumnViewerSorter(column.getViewer(), column, sorter);

		TableColumn[] columns = tableviewer.getTable().getColumns();
		configTableViewer.setContentProvider(new ArrayContentProvider());
		setConfigTableViewerInput(columns);
	}

	private void setConfigTableViewerInput(TableColumn[] columns) {
		configTableViewer.setInput(columns);
		for (TableColumn col: columns)
			configTableViewer.setChecked(col, col.getWidth() > 0);	
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		tableviewer.saveColumnState();
		close();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600,460);
		newShell.setText("Customize Columns");
	}

	protected void toggleColumnVisible(TableColumn column, boolean visible) {
		ColumnConfiguration cfg = (ColumnConfiguration)column.getData();
		if (visible) {
			if (cfg != null && cfg.getWidth() != 0) column.setWidth(cfg.getWidth());
			else column.setWidth(100);
			column.setResizable(true);
		} else {
			column.setWidth(0);
			column.setResizable(false);
		}
	}

}
