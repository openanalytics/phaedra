package eu.openanalytics.phaedra.base.ui.search.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class DateTimeRangeQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.TEMPORAL, Operator.BETWEEN, null))));
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}
	
	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		Serializable value = queryFilter.getValue();
		return value != null && value instanceof Date[] && ((Date[]) value).length == 2 && ((Date[]) value)[0] != null && ((Date[]) value)[1] != null;
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(new Date[] {new Date(), new Date()});
	}

	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		
		DateTime dateTime1 = createDateTime(container, queryEditor, queryFilter, 0);			
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(dateTime1);		
		
		DateTime dateTime2 = createDateTime(container, queryEditor, queryFilter, 1);			
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(dateTime2);		

		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(container);
		
		return container;
	}

	private DateTime createDateTime(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter, final int index) {
		Calendar calendar = Calendar.getInstance();			
		calendar.setTime(((Date[]) queryFilter.getValue())[index]);

		final DateTime dateTime = new DateTime(container, SWT.DATE | SWT.DROP_DOWN);
		dateTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDay());
				calendar.set(Calendar.MONTH, dateTime.getMonth());
				calendar.set(Calendar.YEAR, dateTime.getYear());
				Date[] dates = (Date[]) queryFilter.getValue();
				dates[index] = calendar.getTime();
			}
		});
		dateTime.addSelectionListener(queryEditor.getDirtySelectionAdapter());		
		dateTime.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		return dateTime;
	}	

}
