package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import static eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomColumnSupport.getConfig;

import java.util.Comparator;
import java.util.function.Function;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;


public class CustomizeColumnsDialog extends TitleAreaDialog {
	
	
	protected RichTableViewer targetViewer;
	private CustomColumnSupport customColumnSupport;
	private WritableList<TableColumn> columnList;
	
	protected CheckboxTableViewer configTableViewer;
	
	private Text searchWidget;
	
	private boolean enableMoveSupport;
	private Button moveLeftButton;
	private Button moveRightButton;
	
	private Button addButton;
	private Button editButton;
	private Button deleteButton;
	
	private boolean showColumn;
	
	
	public CustomizeColumnsDialog(Shell parentShell, RichTableViewer tableViewer) {
		super(parentShell);
		this.targetViewer = tableViewer;
		this.customColumnSupport = tableViewer.getCustomColumnSupport();
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		this.columnList= new WritableList<>();
		loadColumns();
		
		this.enableMoveSupport = checkEnableMoveSupport();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(640, 600);
		newShell.setText("Configure Columns");
	}
	
	private void loadColumns() {
		this.columnList.clear();
		this.targetViewer.getColumnConfigs(false); // update configs
		final TableColumn[] columns = targetViewer.getTable().getColumns();
		final int[] columnOrder = targetViewer.getTable().getColumnOrder();
		for (int i = 0; i < columns.length; i++) {
			this.columnList.add(columns[columnOrder[i]]);
		}
	}
	
	private boolean checkEnableMoveSupport() {
		if (this.customColumnSupport != null) {
			return true;
		}
		for (final TableColumn column : columnList) {
			final ColumnConfiguration config = getConfig(column);
			if (config.isMovable()) {
				return true;
			}
		}
		return false;
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Configure Columns");
		setMessage("Configure the displayed columns of the table.");
		
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

		this.configTableViewer.setInput(this.columnList);
		
		Display.getCurrent().asyncExec(() -> {
			this.configTableViewer.setSelection(new StructuredSelection(this.columnList.get(0)));
			updateStatus();
			this.showColumn = true;
		});
		
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
		column.getColumn().setWidth(75);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final ColumnConfiguration config = getConfig((TableColumn)element);
				
				final DataType dataType = config.getDataType();
				return (dataType != null) ? dataType.getLabel() : "unspecified";
			}
		});
		sorter = new Comparator<TableColumn>(){
			@Override
			public int compare(TableColumn o1, TableColumn o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				String dt1 = "" + getConfig(o1).getDataType();
				String dt2 = "" + getConfig(o2).getDataType();
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
		
		configTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatus();
			}
		});
		
		if (this.customColumnSupport != null) {
			this.configTableViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(final DoubleClickEvent event) {
					final IStructuredSelection selection = ((IStructuredSelection)event.getSelection());
					editColumn((TableColumn)selection.getFirstElement());
				}
			});
			this.configTableViewer.getTable().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {
					if (e.keyCode == SWT.DEL) {
						deleteColumn(getSelectedColumn());
					}
				}
			});
		}
		
		return configTableViewer.getTable();
	}
	
	protected TableColumn getSelectedColumn() {
		final IStructuredSelection selection = ((IStructuredSelection)configTableViewer.getSelection());
		return (TableColumn)selection.getFirstElement();
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
		
		if (this.enableMoveSupport) {
			addColumnActionSeparator(composite);
			
			this.moveLeftButton = addColumnActionButton(composite, "\u2191  Move &Left", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveColumn(getSelectedColumn(), -1);
				}
			});
			this.moveRightButton = addColumnActionButton(composite, "\u2193  Move &Right", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveColumn(getSelectedColumn(), +1);
				}
			});
			addColumnActionButton(composite, "Reset Order", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					resetColumnOrder();
				}
			});
		}
		
		if (this.customColumnSupport != null) {
			addColumnActionSeparator(composite);
			Label label = new Label(composite, SWT.NONE);
			label.setText("Custom Column:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			this.addButton = addColumnActionButton(composite, "A&dd...", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					addColumn(customColumnSupport.getDefaultType());
				}
			});
			this.editButton = addColumnActionButton(composite, "&Edit...", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					editColumn(getSelectedColumn());
				}
			});
			this.deleteButton = addColumnActionButton(composite, "&Delete", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					deleteColumn(getSelectedColumn());
				}
			});
		}
		
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
				targetViewer.getTable().setRedraw(false);
				for (TableColumn column : columnList) {
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
	
	protected void updateStatus() {
		final TableColumn column = getSelectedColumn();
		
		if (this.enableMoveSupport) {
			this.moveLeftButton.setEnabled(canMoveColumn(column, -1));
			this.moveRightButton.setEnabled(canMoveColumn(column, +1));
		}
		
		if (this.customColumnSupport != null) {
			this.addButton.setEnabled(this.customColumnSupport.canAddColumn());
			this.editButton.setEnabled(this.customColumnSupport.canEditColumn(column));
			this.deleteButton.setEnabled(this.customColumnSupport.canDelete(column));
		}
		
		if (showColumn && column != null) {
			this.targetViewer.getTable().showColumn(column);
		}
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		targetViewer.updateSearch();
		targetViewer.saveColumnState(true);
		close();
	}
	
	
	protected boolean canMoveColumn(final TableColumn column, final int direction) {
		if (column == null) {
			return false;
		}
		final int currentIndex = this.columnList.indexOf(column);
		final int newIndex = currentIndex + direction;
		if (newIndex < 0 || newIndex >= this.columnList.size()) {
			return false;
		}
		final ColumnConfiguration config= getConfig(column);
//		final ColumnConfiguration newIndexConfig= (ColumnConfiguration)this.columnList.get(newIndex).getData();
		return (config != null && config.isMovable()
//				&& newIndexConfig != null && newIndexConfig.isMovable()
				);
	}
	
	private void moveColumn(final TableColumn column, final int direction) {
		if (!canMoveColumn(column, direction)) {
			return;
		}
		final int currentIndex = this.columnList.indexOf(column);
		final int newIndex = currentIndex + direction;
		final int[] columnOrder = this.targetViewer.getTable().getColumnOrder();
		int saved = columnOrder[newIndex];
		columnOrder[newIndex] = columnOrder[currentIndex];
		columnOrder[currentIndex] = saved;
		this.targetViewer.getTable().setColumnOrder(columnOrder);
		this.columnList.move(currentIndex, newIndex);
		this.configTableViewer.refresh();
		updateStatus();
	}
	
	private void resetColumnOrder() {
		final int[] columnOrder = new int[this.columnList.size()];
		for (int i = 0; i < columnOrder.length; i++) {
			columnOrder[i] = i;
		}
		this.targetViewer.getTable().setColumnOrder(columnOrder);
		loadColumns();
		this.configTableViewer.refresh();
		updateStatus();
	}
	
	protected void toggleColumnVisible(final TableColumn column, final boolean visible) {
		ColumnConfiguration config = getConfig(column);
		config.setHidden(!visible);
//		this.targetViewer.updateColumn(column, config);
		if (visible) {
			column.setWidth((config.getWidth() > 0) ? config.getWidth() : 100);
			column.setResizable(true);
		} else {
			column.setWidth(0);
			column.setResizable(false);
		}
	}
	
	private void addColumn(final String type) {
		TableColumn column = this.customColumnSupport.addColumn(this.targetViewer, type, getShell());
		if (column != null) {
			this.columnList.add(column);
			this.configTableViewer.add(column);
			this.configTableViewer.setSelection(new StructuredSelection(column));
			updateStatus();
		}
	}
	
	private void editColumn(final TableColumn column) {
		boolean ok = this.customColumnSupport.editColumn(this.targetViewer, column, getShell());
		if (ok) {
			this.configTableViewer.refresh(column);
			updateStatus();
		}
	}
	
	private void deleteColumn(final TableColumn column) {
		boolean ok = this.customColumnSupport.deleteColumn(this.targetViewer, column, getShell());
		if (ok) {
			final int index = this.columnList.indexOf(column);
			this.columnList.remove(column);
			this.configTableViewer.remove(column);
			if (!this.columnList.isEmpty()) {
				this.configTableViewer.setSelection(new StructuredSelection(
						this.columnList.get((index < this.columnList.size()) ? index : index - 1) ));
			}
			updateStatus();
		}
	}

}
