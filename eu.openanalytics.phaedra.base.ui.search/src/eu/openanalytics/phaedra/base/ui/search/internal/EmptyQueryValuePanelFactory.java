package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class EmptyQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, null, Operator.EMPTY, null), 
			new QueryFilter(null, null, null, Operator.TRUE, null))));
	
	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		return true;
	}

	@Override
	public void clearValue(QueryFilter queryFilter) {
	}

	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}
	
	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 20).align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(composite);
		
		return composite;
	}

}
