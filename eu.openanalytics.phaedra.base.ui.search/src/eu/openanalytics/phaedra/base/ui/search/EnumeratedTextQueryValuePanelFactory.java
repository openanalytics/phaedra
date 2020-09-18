package eu.openanalytics.phaedra.base.ui.search;

import java.io.Serializable;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;


public abstract class EnumeratedTextQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	public abstract List<String> getEnumeration();
	
	public String getDefaultValue() {
		return null;
	}

	@Override
	public boolean checkValue(final QueryFilter queryFilter) {
		return queryFilter.getValue() != null && queryFilter.getValue() instanceof String && Iterables.any(getEnumeration(), new Predicate<String>() {
				@Override
				public boolean apply(String value) {
					return value.equalsIgnoreCase((String) queryFilter.getValue());
				}
			});
	}

	@Override
	public void clearValue(QueryFilter queryFilter) {
		queryFilter.setValue(null);
	}

	@Override
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(container);
		
		CCombo combo = createCombo(container, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(combo);		
		
		return container;
	}

	protected CCombo createCombo(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		String selection = (String) queryFilter.getValue();
		if (selection == null) {
			queryFilter.setValue(getDefaultValue());
		}
		
		CCombo combo = new CCombo(container, SWT.BORDER | SWT.READ_ONLY);
		final ComboViewer comboViewer = new ComboViewer(combo);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new LabelProvider());
		comboViewer.setInput(getEnumeration());
		comboViewer.setComparator(new ViewerComparator());
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				queryFilter.setValue((Serializable) ((StructuredSelection) comboViewer.getSelection()).getFirstElement());
				queryFilter.setCaseSensitive(false);
			}
		});
		comboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		comboViewer.getCCombo().setVisibleItemCount(Math.min(comboViewer.getCCombo().getItemCount(), 10));
		if (queryFilter.getValue() != null) {
			comboViewer.setSelection(new StructuredSelection(queryFilter.getValue()), true);
		}
		return combo;
	}
		
}
	