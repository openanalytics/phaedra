package eu.openanalytics.phaedra.base.ui.search.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.DateOffsetType;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

public class DateOffsetQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(null, null, OperatorType.TEMPORAL, Operator.IN_LAST, null))));
	
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}
	
	@Override
	public boolean checkValue(QueryFilter queryFilter) {
		Serializable value = queryFilter.getValue();
		return value != null && value instanceof Serializable[] && ((Serializable[]) value).length == 2 && ((Serializable[]) value)[0] != null && ((Serializable[]) value)[1] != null && ((Serializable[]) value)[0] instanceof DateOffsetType && ((Serializable[]) value)[1] instanceof Integer;
	}
	
	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(new Serializable[2]);
	}
	
	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		Text text = createText(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).grab(false, false).applyTo(text);

		CCombo dateOffsetTypeCombo = createDateOffsetType(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(dateOffsetTypeCombo);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(container);
		
		return container;
	}
	
	private Text createText(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		Integer value = (Integer) ((Serializable[]) queryFilter.getValue())[1];		
		
		final Text text = new Text(container, SWT.SINGLE | SWT.BORDER);		
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					((Serializable[]) queryFilter.getValue())[1] = Integer.parseInt(text.getText());
				} catch (Exception ex) {
				}
			}
		});
		text.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				text.setText(queryFilter.getValue() == null ? "" : "" + ((Serializable[]) queryFilter.getValue())[1]);
			}
		});
		text.addKeyListener(queryEditor.getDirtyKeyAdapter());
		if (value != null) {
			text.setText(Integer.toString(value));
		}
		return text;
	} 
	
	private CCombo createDateOffsetType(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		DateOffsetType value = (DateOffsetType) ((Serializable[]) queryFilter.getValue())[0];
		
		CCombo combo = new CCombo(container, SWT.BORDER | SWT.READ_ONLY);
				
		final ComboViewer comboViewer = new ComboViewer(combo);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((DateOffsetType) element).getName();
			}
		});		
		comboViewer.setInput(DateOffsetType.values());
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				((Serializable[]) queryFilter.getValue())[0] = (DateOffsetType) ((IStructuredSelection) comboViewer.getSelection()).getFirstElement();
			}
		});
		if (value != null) {
			comboViewer.setSelection(new StructuredSelection(value));
		}
		return combo;
	}
}
