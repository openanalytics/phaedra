package eu.openanalytics.phaedra.base.ui.util.table;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;

public class FilteredCheckboxTable extends FilteredTable {

	public FilteredCheckboxTable(Composite parent, int tableStyle, FilterMatcher filter, boolean useNewLook) {
		super(parent, tableStyle, filter, useNewLook);
	}

	@Override
	protected TableViewer doCreateTableViewer(Composite parent, int style) {
		return CheckboxTableViewer.newCheckList(parent, style);
	}

	public CheckboxTableViewer getCheckboxViewer() {
		return (CheckboxTableViewer) getViewer();
	}

}
