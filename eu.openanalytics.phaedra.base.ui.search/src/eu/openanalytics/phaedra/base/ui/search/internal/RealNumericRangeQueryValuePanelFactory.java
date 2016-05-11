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

public class RealNumericRangeQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.BETWEEN, null)));
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		return queryFilter.getValue() != null && queryFilter.getValue() instanceof Double[] && ((Double[]) queryFilter.getValue()).length == 2;
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(new Double[2]);
	}
	
	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
	
		Text text1 = createText(container, queryEditor, queryFilter, 0);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text1);
		
		Text text2 = createText(container, queryEditor, queryFilter, 1);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text2);

		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, true).applyTo(container);
		
		return container;
	}

	private Text createText(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter, final int index) {
		Double value = ((Double[]) queryFilter.getValue())[index];		
		
		final Text text = new Text(container, SWT.SINGLE | SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					Double[] values = (Double[]) queryFilter.getValue();
					values[index] = Double.parseDouble(text.getText());
				} catch (Exception ex) {
				}
			}
		});
		text.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				Double[] value = (Double[]) queryFilter.getValue();
				text.setText(value[index] == null ? "" : "" + value[index]);
			}
		});
		text.addKeyListener(queryEditor.getDirtyKeyAdapter());		
		if (value != null) {
			text.setText(Double.toString(value));
		}
		return text;
	}

}
