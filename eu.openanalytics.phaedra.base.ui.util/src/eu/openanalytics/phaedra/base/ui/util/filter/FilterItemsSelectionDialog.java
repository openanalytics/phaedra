package eu.openanalytics.phaedra.base.ui.util.filter;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class FilterItemsSelectionDialog extends TitleAreaDialog {

		private String title;
		private String message;

		private CheckboxTableViewer configTableViewer;

		private List<String> filterItems;
		private List<String> activeFilterItems;

		public FilterItemsSelectionDialog(String title, String message
				, List<String> filterItems, List<String> activeFilterItems) {

			super(Display.getDefault().getActiveShell());

			this.title = title;
			this.message = message;

			this.filterItems = filterItems;
			this.activeFilterItems = activeFilterItems;
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText(title);
			shell.setSize(350, 550);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);

			setTitle(title);
			setMessage(message);

			final Composite compositeColumns = new Composite(area, SWT.NONE);
			GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(compositeColumns);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(compositeColumns);

			final Composite compositeLeft = new Composite(compositeColumns, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(compositeLeft);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(compositeLeft);

			configTableViewer = CheckboxTableViewer.newCheckList(compositeLeft, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			configTableViewer.setContentProvider(new ArrayContentProvider());
			final Table configTable = configTableViewer.getTable();

			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(configTable);

			Composite compositeRight = new Composite(compositeColumns, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(compositeRight);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeRight);

			final Button buttonAll = new Button(compositeRight, SWT.NONE);
			buttonAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			buttonAll.setText("Select All");
			buttonAll.setToolTipText("Select everything");
			buttonAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					configTableViewer.setAllChecked(true);
				}
			});

			final Button buttonNone = new Button(compositeRight, SWT.NONE);
			buttonNone.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			buttonNone.setText("Select None");
			buttonNone.setToolTipText("Select nothing");
			buttonNone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					configTableViewer.setAllChecked(false);
				}
			});

			configTableViewer.setInput(filterItems);
			for (String type : activeFilterItems) {
				configTableViewer.setChecked(type, true);
			}

			return area;
		}

		@Override
		protected void okPressed() {
			activeFilterItems.clear();
			for (String type : filterItems) {
				if (configTableViewer.getChecked(type)) {
					activeFilterItems.add(type);
				}
			}

			super.okPressed();
		}

		public List<String> getActiveFilterItems() {
			return activeFilterItems;
		}
	}