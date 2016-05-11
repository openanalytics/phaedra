package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class RealNumericQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.EQUALS, null),
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.GREATER_THAN, null),
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.LESS_THAN, null)));
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		return queryFilter.getValue() == null || queryFilter.getValue() instanceof Double;
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(null);
	}
	
	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
	
		Text text = createText(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().grab(true, false).hint(80, SWT.DEFAULT).applyTo(text);
		
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, true).applyTo(container);
		
		return container;
	}

	private Text createText(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		final Text text = new Text(container, SWT.SINGLE | SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					queryFilter.setValue(Double.parseDouble(text.getText()));
				} catch (Exception ex) {
				}
			}
		});
		text.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				text.setText(queryFilter.getValue() == null ? "" : "" + queryFilter.getValue());
			}
		});
		text.addKeyListener(queryEditor.getDirtyKeyAdapter());
		if (queryFilter.getValue() != null) {
			text.setText(Double.toString((Double) queryFilter.getValue()));
		}	
		return text;
	}

}
