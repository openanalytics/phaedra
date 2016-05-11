package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;
import eu.openanalytics.phaedra.base.ui.util.table.FilteredTable;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class ColumnFilter {

	private Composite area;
	
	private FilteredTable excludeTable;
	private FilteredTable includeTable;
	
	private WritableList excludeList;
	private WritableList includeList;
	
	public ColumnFilter(Composite parent, String[] items, String includeText, String excludeText) {
		
		excludeList = new WritableList(new ArrayList<String>(), String.class);
		includeList = new WritableList(Arrays.stream(items).collect(Collectors.toList()), String.class);
		
		area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(area);
		
		/*
		 * Exclude group
		 */
		
		Group gExclude = new Group(area, SWT.NONE);
		gExclude.setText(excludeText);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(gExclude);
		GridDataFactory.fillDefaults().grab(true,true).hint(150, 250).applyTo(gExclude);
		
		excludeTable = new FilteredTable(gExclude, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, new FilterMatcher(), true);
		
		TableViewer excludeTableViewer = excludeTable.getViewer();
		excludeTableViewer.setContentProvider(new ArrayContentProvider());
		excludeTableViewer.setInput(excludeList);
		
		TableViewerColumn tvc = new TableViewerColumn(excludeTableViewer, SWT.NONE);
		tvc.getColumn().setWidth(350);
		tvc.setLabelProvider(new ColumnLabelProvider());
		
		/*
		 * Button group
		 */
		
		Group gButtons = new Group(area, SWT.NONE);
		gButtons.setText("Select");
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(gButtons);
		GridDataFactory.fillDefaults().grab(false,true).hint(SWT.DEFAULT, 250).applyTo(gButtons);

		Button bAdd = new Button(gButtons, SWT.PUSH);
		bAdd.setText("Add >>");
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).align(SWT.CENTER, SWT.CENTER).applyTo(bAdd);

		Button bAddAll = new Button(gButtons, SWT.PUSH);
		bAddAll.setText("Add All >>");
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).align(SWT.CENTER, SWT.CENTER).applyTo(bAddAll);

		Button bRemove = new Button(gButtons, SWT.PUSH);
		bRemove.setText("<< Remove");
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).align(SWT.CENTER, SWT.CENTER).applyTo(bRemove);

		Button bRemoveAll = new Button(gButtons, SWT.PUSH);
		bRemoveAll.setText("<< Remove All");
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).align(SWT.CENTER, SWT.CENTER).applyTo(bRemoveAll);

		/*
		 * Include group
		 */
		
		Group gInclude = new Group(area, SWT.NONE);
		gInclude.setText(includeText);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(gInclude);
		GridDataFactory.fillDefaults().grab(true,true).hint(150, 250).applyTo(gInclude);
		
		includeTable = new FilteredTable(gInclude, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, new FilterMatcher(), true);
		
		TableViewer includeTableViewer = includeTable.getViewer();
		includeTableViewer.setContentProvider(new ArrayContentProvider());
		includeTableViewer.setInput(includeList);

		tvc = new TableViewerColumn(includeTableViewer, SWT.NONE);
		tvc.getColumn().setWidth(350);
		tvc.setLabelProvider(new ColumnLabelProvider());
		
		/*
		 * Button listeners
		 */
		
		bAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<String> itemsToAdd = SelectionUtils.getObjects(excludeTableViewer.getSelection(), String.class);
				includeList.addAll(itemsToAdd);
				excludeList.removeAll(itemsToAdd);
				includeTableViewer.refresh();
				excludeTableViewer.refresh();
			}
		});

		bAddAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeList.addAll(excludeList);
				excludeList.clear();
				includeTableViewer.refresh();
				excludeTableViewer.refresh();
			}
		});

		bRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<String> itemsToRemove = SelectionUtils.getObjects(includeTableViewer.getSelection(), String.class);
				includeList.removeAll(itemsToRemove);
				excludeList.addAll(itemsToRemove);
				includeTableViewer.refresh();
				excludeTableViewer.refresh();
			}
		});

		bRemoveAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				excludeList.addAll(includeList);
				includeList.clear();
				includeTableViewer.refresh();
				excludeTableViewer.refresh();
			}
		});
	}
	
	public Composite getControl() {
		return area;
	}
	
	public String[] getIncluded() {
		return (String[]) includeList.toArray(new String[includeList.size()]);
	}
	
	public String[] getExcluded() {
		return (String[]) excludeList.toArray(new String[excludeList.size()]);
	}
}
