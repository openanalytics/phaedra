package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.Comparator;
import java.util.function.Function;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;


public class CustomizeColumnsDialog extends TitleAreaDialog {
	
	
	protected CheckboxTableViewer configTableViewer;
	
	private Text searchWidget;
	
	protected RichTableViewer targetViewer;
	
	
	public CustomizeColumnsDialog(Shell parentShell, RichTableViewer tableViewer) {
		super(parentShell);
		this.targetViewer = tableViewer;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600, 460);
		newShell.setText("Configure Columns");
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Configure Columns");
		setMessage("Configure the table columns.");
		
		Composite dialogArea = (Composite)super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().applyTo(dialogArea);
		
		Composite defaultContainer= new Composite(dialogArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(defaultContainer);
		defaultContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(defaultContainer);

		final Composite compositeLeft = new Composite(defaultContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(compositeLeft);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(compositeLeft);

		final Text textSearch = new Text(compositeLeft, SWT.BORDER | SWT.SEARCH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true,false).applyTo(textSearch);
		
		textSearch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateFilter();
			}
		});
		this.searchWidget= textSearch;

		Composite tableComposite = createConfigTable(compositeLeft);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableComposite);
		
		Composite buttonComposite = createColumnActionButtons(defaultContainer);
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		this.configTableViewer.setInput(targetViewer.getTable().getColumns());
		
		return dialogArea;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Composite createConfigTable(final Composite parent) {
		configTableViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		configTableViewer.getTable().setLinesVisible(true);
		configTableViewer.getTable().setHeaderVisible(true);
		
		TableViewerColumn column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setText("Column");
		column.getColumn().setWidth(150);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TableColumn)element).getText();
			}
		});
		Comparator<?> sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				return o1.getText().compareToIgnoreCase(o2.getText());
			}
		};
		new ColumnViewerSorter(column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setText("Data Type");
		column.getColumn().setWidth(80);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ColumnConfiguration config = (ColumnConfiguration)((TableColumn)element).getData();
				
				final DataType dataType = config.getDataType();
				return (dataType != null) ? dataType.getLabel() : "unspecified";
			}
		});
		sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				String dt1 = ((ColumnConfiguration) o1.getData()).getDataType() + "";
				String dt2 = ((ColumnConfiguration) o2.getData()).getDataType() + "";
				return dt1.compareToIgnoreCase(dt2);
			}
		};
		new ColumnViewerSorter(column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setText("Description");
		column.getColumn().setWidth(250);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
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
				return o1.getToolTipText().compareToIgnoreCase(o2.getToolTipText());
			}
		};
		new ColumnViewerSorter(column, sorter);

		configTableViewer.setContentProvider(new ArrayContentProvider());
		
		configTableViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				return false;
			}
			@Override
			public boolean isChecked(Object element) {
				TableColumn column = (TableColumn)element;
				return (column.getWidth() > 0);
			}
		});
		configTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				TableColumn column = (TableColumn) event.getElement();
				toggleColumnVisible(column, event.getChecked());
			}
		});
		
		return configTableViewer.getTable();
	}
	
	private void updateFilter() {
		String text = searchWidget.getText();
		if (text != null && !text.isEmpty()) {
			configTableViewer.setFilters(new ViewerFilter() {
				final String pattern= text.toLowerCase();
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					TableColumn column = (TableColumn) element;
					return column.getText().toLowerCase().contains(pattern);
				}
			});
		} else {
			configTableViewer.resetFilters();
		}
	}
	
	protected Composite createColumnActionButtons(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(composite);

		final Button buttonSearch = new Button(composite, SWT.NONE);
		buttonSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonSearch.setText("Clear search");
		buttonSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				searchWidget.setText("");
				updateFilter();
			}
		});
		
		addShowHideActionButtons(composite);
		
		return composite;
	}
	
	protected void addShowHideActionButtons(final Composite composite) {
		addColumnActionButton(composite, "&Hide All",
				createChangeColumnVisibleAction((final TableColumn column) -> Boolean.FALSE) );
		addColumnActionButton(composite, "Show &All",
				createChangeColumnVisibleAction((final TableColumn column) -> Boolean.TRUE) );
	}
	
	protected void addColumnActionSeparator(final Composite buttonComposite) {
		GridDataFactory.fillDefaults()
				.hint(-1, convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING) / 2)
				.applyTo(new Label(buttonComposite, SWT.NONE));
	}
	
	protected Button addColumnActionButton(final Composite buttonComposite,
			final String text, final String tooltip, final SelectionListener listener) {
		final Button button = new Button(buttonComposite, SWT.NONE);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setText(text);
		button.setToolTipText(tooltip);
		button.addSelectionListener(listener);
		return button;
	}
	
	protected Button addColumnActionButton(final Composite buttonComposite,
			final String text, final SelectionListener listener) {
		return addColumnActionButton(buttonComposite, text, null, listener);
	}
	
	protected SelectionListener createChangeColumnVisibleAction(final Function<TableColumn, Boolean> stateProvider) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				TableColumn[] columns = targetViewer.getTable().getColumns();
				targetViewer.getTable().setRedraw(false);
				for (TableColumn column : columns) {
					Boolean state= stateProvider.apply(column);
					if (state != null) {
						toggleColumnVisible(column, state);
						configTableViewer.update(column, null);
					}
				}
				targetViewer.getTable().setRedraw(true);
			}
		};
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		targetViewer.saveColumnState();
		close();
	}
	
	
	protected void toggleColumnVisible(TableColumn column, boolean visible) {
		ColumnConfiguration config = (ColumnConfiguration)column.getData();
		if (visible) {
			column.setWidth((config.getWidth() > 0) ? config.getWidth() : 100);
			column.setResizable(true);
		} else {
			column.setWidth(0);
			column.setResizable(false);
		}
	}

}
