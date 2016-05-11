package eu.openanalytics.phaedra.base.ui.search.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class RealNumericsQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.IN, null)));
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		Serializable value = queryFilter.getValue();
		return value != null && value instanceof ArrayList && ( ((ArrayList<?>) value).isEmpty() || ((ArrayList<?>) value).get(0) instanceof Double);
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(new ArrayList<Double>());
	}
	
	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
	
		Text text = createText(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(container);
		
		return container;
	}

	private Text createText(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		final Text text = new Text(container, SWT.SINGLE | SWT.BORDER);
		text.setToolTipText("Add a comma-separated list of values");
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					ArrayList<String> stringValues = new ArrayList<>(Arrays.asList(text.getText().split("\\s*,\\s*")));					
					Collection<Double> doubleValues = Collections2.transform(stringValues, new Function<String, Double>() {
						@Override
						public Double apply(String value) {
							return Double.parseDouble(value);
						}
					});					
					queryFilter.setValue(new ArrayList<>(doubleValues));
				} catch (Exception ex) {
				}
			}
		});
		text.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				updateText(text, queryFilter);
			}
		});
		text.addKeyListener(queryEditor.getDirtyKeyAdapter());
		updateText(text, queryFilter);
		return text;
	}

	private void updateText(Text text, QueryFilter queryFilter) {
		text.setText(queryFilter.getValue().toString().replaceAll("[\\[\\]]", ""));
	}
}
