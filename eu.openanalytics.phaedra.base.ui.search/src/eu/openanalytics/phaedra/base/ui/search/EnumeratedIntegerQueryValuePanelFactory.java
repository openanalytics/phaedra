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


public abstract class EnumeratedIntegerQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	public abstract List<Integer> getEnumeration();
	
	public Integer getDefaultValue() {
		return null;
	}
	
	public abstract LabelProvider getLabelProvider(); 
	
	@Override
	public boolean checkValue(final QueryFilter queryFilter) {
		final Serializable serializable = queryFilter.getValue();
		return serializable != null && serializable instanceof Long && Iterables.any(getEnumeration(), new Predicate<Integer>() {
			@Override
			public boolean apply(Integer value) {
				return serializable.equals(value.longValue());
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

	private CCombo createCombo(Composite container, QueryEditor queryEditor, final QueryFilter queryFilter) {
		Long selection = (Long) queryFilter.getValue();
		if (selection == null) {
			queryFilter.setValue(getDefaultValue());
		}
		
		CCombo combo = new CCombo(container, SWT.BORDER | SWT.READ_ONLY);
		final ComboViewer comboViewer = new ComboViewer(combo);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(getLabelProvider());
		comboViewer.setInput(getEnumeration());
		comboViewer.setComparator(new ViewerComparator());
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Integer selection = (Integer) ((StructuredSelection) comboViewer.getSelection()).getFirstElement();
				queryFilter.setValue(selection.longValue());
			}
		});
		comboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		comboViewer.getCCombo().setVisibleItemCount(Math.min(comboViewer.getCCombo().getItemCount(), 10));
		if (queryFilter.getValue() != null) {
			Integer value = ((Long) queryFilter.getValue()).intValue();
			comboViewer.setSelection(new StructuredSelection(value), true);
		}
		return combo;
	}
		
}
	