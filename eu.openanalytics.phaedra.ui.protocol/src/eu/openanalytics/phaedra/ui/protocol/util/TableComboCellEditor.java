package eu.openanalytics.phaedra.ui.protocol.util;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class TableComboCellEditor extends CellEditor {

	private TableComboViewer viewer;
	private Object selectedValue;

	public TableComboCellEditor(Composite parent) {
		super(parent);
	}
	
	@Override
	protected void doSetValue(Object value) {
		Assert.isTrue(viewer != null);
		selectedValue = value;
		if (value == null) {
			viewer.setSelection(StructuredSelection.EMPTY);
		} else {
			viewer.setSelection(new StructuredSelection(value));
		}
	}

	@Override
	protected void doSetFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	protected Object doGetValue() {
		return selectedValue;
	}

	@Override
	protected Control createControl(Composite parent) {
		viewer = new TableComboViewer(parent, SWT.BORDER | SWT.READ_ONLY);

		viewer.getTableCombo().addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}

			public void widgetSelected(SelectionEvent event) {
				ISelection selection = viewer.getSelection();
				if (selection.isEmpty()) {
					selectedValue = null;
				} else {
					selectedValue = ((IStructuredSelection) selection)
							.getFirstElement();
				}
			}
		});

		viewer.getTableCombo().addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		viewer.getTableCombo().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				TableComboCellEditor.this.focusLost();
			}
		});
		
		return viewer.getTableCombo();
	}

	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		viewer.setLabelProvider(labelProvider);
	}

	public void setContentProvider(IStructuredContentProvider provider) {
		viewer.setContentProvider(provider);
	}

	public void setInput(Object input) {
		viewer.setInput(input);
	}

	public TableComboViewer getViewer() {
		return viewer;
	}
	
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		ISelection selection = viewer.getSelection();
		if (selection.isEmpty()) {
			selectedValue = null;
		} else {
			selectedValue = ((IStructuredSelection) selection)
					.getFirstElement();
		}

		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			MessageFormat.format(getErrorMessage(),
					new Object[] { selectedValue });
		}

		fireApplyEditorValue();
		deactivate();
	}
}
