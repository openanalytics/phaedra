package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.util.misc.SearchBar;
import eu.openanalytics.phaedra.base.ui.util.misc.SearchBar.ISearchHandler;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;

public class NewFeatureTableViewer extends TableViewer{

	// We use icons
	private static final Image CHECKED   = IconManager.getIconDescriptor("tick.png").createImage();
	private static final Image UNCHECKED = IconManager.getIconDescriptor("notification-close-active.gif").createImage();

	private SearchBar searchBar;
	private ClassspecificViewerFilter searchFilter;
	
	public NewFeatureTableViewer(Composite parent)
	{
		super(createTableContainer(parent, true), SWT.MULTI | SWT.H_SCROLL| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createPartControl();
	}
	
	public void createPartControl() {
		
		// Search filter toolbar 
		Composite searchArea = (Composite)getTable().getParent().getParent().getChildren()[0];
		addToolbar(searchArea);
		searchFilter = new ClassspecificViewerFilter();
		this.addFilter(searchFilter);

		// Create the columns 
		createColumns();
		
		// Make lines and make header visible
		final Table table = this.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// Set the ContentProvider
		this.setContentProvider(ArrayContentProvider.getInstance());
	}

	private static Composite createTableContainer(Composite parent, boolean searchEnabled) {
		if (!searchEnabled) return parent;

		Composite c = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(c);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(c);
		
		Composite toolBar = new Composite(c, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(toolBar);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(0,0).spacing(0,0).applyTo(toolBar);

		Composite tableArea = new Composite(c, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableArea);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		return tableArea;
	}

	@SuppressWarnings("unchecked")
	public List<FeatureDefinition> getNewFeatureDefinitions()
	{
		return (List<FeatureDefinition>)getInput();
	}

	public void setFeatureDefinitions(List<FeatureDefinition> FeatureDefinitions)
	{
		setInput(FeatureDefinitions);
	}
	
	// check or uncheck all visible items
	public void setAddFeatureToProtocolClass_all(boolean b)
	{
		TableItem[] visibleItems = getTable().getItems();
		
		for(TableItem item : visibleItems)
			((FeatureDefinition)item.getData()).addFeatureToProtocolClass = b;
		this.refresh();
	}

	// toggle checked state of all visible items
	public void toggleAddFeatureToProtocolClass_all()
	{

		TableItem[] visibleItems = getTable().getItems();
		
		for(TableItem item : visibleItems)
		{
			FeatureDefinition wfd = ((FeatureDefinition)item.getData());
			wfd.addFeatureToProtocolClass = !wfd.addFeatureToProtocolClass;
		}
		this.refresh();
	}
	
	// This will create the columns for the table
	private void createColumns() {
		String[] titles = { "Add to Protocol-class?", "Name", "Is Key?", "Is Numeric?" };
		int[] bounds = { 150, 200, 70, 100 };
		int colIdx = 0;

		// add to protocol-class
		TableViewerColumn col = createTableViewerColumn(titles[colIdx], bounds[colIdx], colIdx);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (((FeatureDefinition) element).addFeatureToProtocolClass) {
					return CHECKED;
				} else {
					return UNCHECKED;
				}
			}
		});
		col.setEditingSupport(new FeatureAddToProtocolClassEditingSupport(this));
		new ColumnViewerSorter<>(this, col, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				Boolean b1 = ((FeatureDefinition)o1).addFeatureToProtocolClass;
				Boolean b2 = ((FeatureDefinition)o2).addFeatureToProtocolClass;
				return b1.compareTo(b2);
			};
		});
		colIdx++;

		// name
		col = createTableViewerColumn(titles[colIdx], bounds[colIdx], colIdx);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				FeatureDefinition p = (FeatureDefinition) element;
				return p.name;
			}
		});
		new ColumnViewerSorter<>(this, col, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				String s1 = ((FeatureDefinition)o1).name;
				String s2 = ((FeatureDefinition)o2).name;
				if (s1 == null && s2 != null) return -1;
				if (s1 != null && s2 == null) return 1;
				return s1.compareTo(s2);
			};
		});
		colIdx++;
		
		// Is Key?
		col = createTableViewerColumn(titles[colIdx], bounds[colIdx], colIdx);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (((FeatureDefinition) element).isKey) {
					return CHECKED;
				} else {
					return UNCHECKED;
				}
			}
		});
		col.setEditingSupport(new FeatureIsKeyEditingSupport(this));
		new ColumnViewerSorter<>(this, col, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				Boolean b1 = ((FeatureDefinition)o1).isKey;
				Boolean b2 = ((FeatureDefinition)o2).isKey;
				return b1.compareTo(b2);
			};
		});
		colIdx++;

		// Is Numeric?
		col = createTableViewerColumn(titles[colIdx], bounds[colIdx], colIdx);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (((FeatureDefinition) element).isNumeric) {
					return CHECKED;
				} else {
					return UNCHECKED;
				}
			}
		});
		col.setEditingSupport(new FeatureIsNumericEditingSupport(this));
		new ColumnViewerSorter<>(this, col, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				Boolean b1 = ((FeatureDefinition)o1).isNumeric;
				Boolean b2 = ((FeatureDefinition)o2).isNumeric;
				return b1.compareTo(b2);
			};
		});
		colIdx++;
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(this, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}
	
	private void addToolbar(Composite parent) {
		
		//********************************************************
		//
		//                   search bar
		//
		//*********************************************************

		Composite panel = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(panel);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(panel);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(panel);

		searchBar = new SearchBar(panel, SWT.NONE, true);	
		GridDataFactory.fillDefaults().applyTo(searchBar);

		searchBar.setNames(new String[]{"Name"});
		searchBar.setSearchHandler(new ISearchHandler() {
			@Override
			public void doSearch(final String name, final String value) {
				searchFilter.setSearchText(value);
				refresh(true);
			}

			@Override
			public void doClear() {
				searchFilter.setSearchText(null);
			}
		});
		
		//********************************************************
		//
		//      check all / uncheck all / invert buttons
		//
		//*********************************************************
		
		Composite panelButtons = new Composite(panel, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(panelButtons);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(panelButtons);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(panelButtons);
		
		Button btnCheckAll = new Button(panelButtons, SWT.PUSH);
		btnCheckAll.setText("Check All");
		GridDataFactory.fillDefaults().grab(false,false).applyTo(btnCheckAll);
		btnCheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setAddFeatureToProtocolClass_all(true);
			}
		});
		
		Button btnUncheckAll = new Button(panelButtons, SWT.PUSH);
		btnUncheckAll.setText("Uncheck All");
		GridDataFactory.fillDefaults().grab(false,false).applyTo(btnUncheckAll);
		btnUncheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setAddFeatureToProtocolClass_all(false);
			}
		});

		Button btnToggle = new Button(panelButtons, SWT.PUSH);
		btnToggle.setText("Invert Checked");
		GridDataFactory.fillDefaults().grab(false,false).applyTo(btnToggle);
		btnToggle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				toggleAddFeatureToProtocolClass_all();
			}
		});
	}
	
	private class ClassspecificViewerFilter extends ViewerFilter {
		
		private Pattern pattern;
		
		public void setSearchText(String text){
			if (text == null || text.isEmpty()) {
				pattern = null;
			} else {
				String regex = text.toLowerCase().replace("(", "\\(").replace(")", "\\)").replace(".", "\\.").replace("*", ".*");
				pattern = Pattern.compile(".*" + regex + ".*");
			}
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (pattern == null) return true;
			FeatureDefinition elem = (FeatureDefinition)element;
			return pattern.matcher(elem.name.toLowerCase()).matches();
			
		}
	}

	private class FeatureAddToProtocolClassEditingSupport extends EditingSupport {

		private final TableViewer viewer;

		public FeatureAddToProtocolClassEditingSupport(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
		}


		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((FeatureDefinition) element).addFeatureToProtocolClass;
		}

		@Override
		protected void setValue(Object element, Object value) {
			((FeatureDefinition) element).addFeatureToProtocolClass = (boolean)value;
			viewer.refresh();
		}
	}
	private class FeatureIsKeyEditingSupport extends EditingSupport {

		private final TableViewer viewer;

		public FeatureIsKeyEditingSupport(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
		}


		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((FeatureDefinition) element).isKey;
		}

		@Override
		protected void setValue(Object element, Object value) {
			((FeatureDefinition) element).isKey = (boolean)value;
			viewer.refresh();
		}
	}
	
	private class FeatureIsNumericEditingSupport extends EditingSupport {

		private final TableViewer viewer;

		public FeatureIsNumericEditingSupport(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
		}


		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((FeatureDefinition) element).isNumeric;
		}

		@Override
		protected void setValue(Object element, Object value) {
			((FeatureDefinition) element).isNumeric = (boolean)value;
			viewer.refresh();
		}
	}
}
