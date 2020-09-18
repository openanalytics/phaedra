package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedTextQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;


public class WellTypeQueryValuePanelFactory extends EnumeratedTextQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(ProtocolClass.class, "highWellTypeCode", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(ProtocolClass.class, "lowWellTypeCode", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(Well.class, "wellType", OperatorType.STRING, Operator.STRING_EQUALS, null))));

	private static Map<String, WellType> WELL_TYPE_CODES = ProtocolService.getInstance().getWellTypes().stream()
			.collect(Collectors.toMap(wellType -> wellType.getCode(), wellType -> wellType));
	
	@Override
	public Set<QueryFilter> getFilters() {
		
		return FILTERS;
	}

	@Override
	public List<String> getEnumeration() {
		return Collections.emptyList();
	}

	/**
	 * Part of the PHA-644 implementation 
	 */
	@Override
	protected CCombo createCombo(Composite container, QueryEditor queryEditor, QueryFilter queryFilter) {
		String selection = (String) queryFilter.getValue();
		if (selection == null) {
			queryFilter.setValue(getDefaultValue());
		}
		
		CCombo combo = new CCombo(container, SWT.BORDER | SWT.READ_ONLY);
		ComboViewer comboViewer = new ComboViewer(combo);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setInput(WELL_TYPE_CODES.values());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				WellType wellType = (WellType)element;
				return ProtocolUtils.getCustomHCLCLabel(wellType.getCode());
			}
		});
		comboViewer.setComparator(new ViewerComparator());
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				queryFilter.setValue(((WellType) ((StructuredSelection) comboViewer.getSelection()).getFirstElement()).getCode());
				queryFilter.setCaseSensitive(false);
			}
		});
		comboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		comboViewer.getCCombo().setVisibleItemCount(Math.min(comboViewer.getCCombo().getItemCount(), 10));
		if (queryFilter.getValue() != null) {
			comboViewer.setSelection(new StructuredSelection(WELL_TYPE_CODES.get(queryFilter.getValue())), true);
		}
		return combo;
	}
	
	

}
