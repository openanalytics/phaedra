package eu.openanalytics.phaedra.base.ui.search.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class TextsQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.STRING, Operator.STRING_IN, null))));
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}
	
	public boolean checkValue(QueryFilter queryFilter) {
		Serializable value = queryFilter.getValue();
		return value != null && value instanceof ArrayList && ( ((ArrayList<?>) value).isEmpty() || ((ArrayList<?>) value).get(0) instanceof String);
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(new ArrayList<String>());
	}

	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		Text text = createText(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

		Button checkbox = createCheckbox(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(checkbox);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(container);
		
		return container;
	}
	
	private Text createText(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		final Text text = new Text(container, SWT.SINGLE | SWT.BORDER);
		text.setToolTipText("Add a comma-separated list of values");
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				queryFilter.setValue(new ArrayList<>(Arrays.asList(text.getText().split("\\s*,\\s*"))));
			}
		});
		text.addKeyListener(queryEditor.getDirtyKeyAdapter());
		updateText(text, queryFilter);
		return text;
	}
	
	private Button createCheckbox(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		final Button checkbox = new Button(container, SWT.CHECK);
		checkbox.setText("Case sensitive");
		checkbox.setSelection(queryFilter.isCaseSensitive());
		checkbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				queryFilter.setCaseSensitive(checkbox.getSelection());
			}
		});
		checkbox.addSelectionListener(queryEditor.getDirtySelectionAdapter());
		checkbox.setSelection(queryFilter.isCaseSensitive());
		return checkbox;
	}
	
	private void updateText(Text text, QueryFilter queryFilter) {
		text.setText(queryFilter.getValue() == null ? "" : "" + queryFilter.getValue().toString().replaceAll("[\\[\\]]", ""));
	}
}
