package eu.openanalytics.phaedra.ui.curve.prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider.CurveTextField;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.ui.curve.Activator;
import eu.openanalytics.phaedra.ui.curve.details.CrcDetailsView;


public class PropertyPreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	
	
	private List<String> availableProperties;
	
	private WritableList<String> topProperties;
	
	private TableViewer propertyTableViewer;
	private Button addButton;
	private Button removeButton;
	private Button upButton;
	private Button downButton;
	
	
	public PropertyPreferencePage() {
		topProperties = new WritableList<String>();
		
		availableProperties = loadAvailableProperties();
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}
	
	private List<String> loadAvailableProperties() {
		List<String> availableProperties = new ArrayList<>();
		for (CurveTextField column : CurveTextProvider.getColumns(null)) {
			availableProperties.add(column.getLabel());
		}
		Set<String> unique = new HashSet<String>();
		for (String modelId : CurveFitService.getInstance().getFitModels()) {
			for (CurveParameter.Definition def : CurveFitService.getInstance().getModel(modelId).getOutputParameters(null)) {
				unique.add(def.name);
			}
		}
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		Curve curve = (part instanceof CrcDetailsView) ? ((CrcDetailsView) part).getCurve() : null;
		if (curve != null) {
			for (Value value : curve.getOutputParameters()) {
				unique.add(value.definition.name);
			}
		}
		ArrayList<String> sorted = new ArrayList<>(unique);
		sorted.sort(null);
		availableProperties.addAll(sorted);
		return availableProperties;
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
	
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Composite propertiesComposite = createPropertyTable(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(propertiesComposite);
		
		load();
		composite.getDisplay().asyncExec(() -> {
			if (propertyTableViewer.getControl() == null || propertyTableViewer.getControl().isDisposed()) return;
			if (!topProperties.isEmpty()) {
				propertyTableViewer.setSelection(new StructuredSelection(topProperties.get(0)));
			}
		});
		
		return composite;
	}
	
	protected Composite createPropertyTable(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("Add, remove and arrange favored properties shown at the top of the table:");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
		
		TableViewer viewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider());
		column.getColumn().setWidth(convertWidthInCharsToPixels(40));
		viewer.setInput(this.topProperties);
		GridDataFactory.fillDefaults().grab(true, true)
				.hint(column.getColumn().getWidth() + 16, convertHeightInCharsToPixels(10))
				.applyTo(viewer.getControl());
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updatePropertyButtons();
			}
		});
		this.propertyTableViewer = viewer;
		
		Composite buttons = new Composite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttons);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(buttons);
		
		addButton = addButton(buttons, "Add property...", this::addProperty);
		removeButton = addButton(buttons, "Remove", this::removeProperty);
		upButton = addButton(buttons, "Up", this::movePropertyUp);
		downButton = addButton(buttons, "Down", this::movePropertyDown);
		
		return composite;
	}
	
	private Button addButton(Composite parent, String text, Runnable listener) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listener.run();
			}
		});
		GridDataFactory.defaultsFor(button).applyTo(button);
		return button;
	}
	
	private void addProperty() {
		List<String> properties = new ArrayList<>(availableProperties);
		properties.removeAll(topProperties);
		ListSelectionDialog input = new ListSelectionDialog(getShell(), properties, new ArrayContentProvider(), new LabelProvider(), null);
		input.setTitle("Add Property");
		input.setMessage("Select the properties to add:");
		List<String> result;
		if (input.open() == Dialog.OK && (result = (List)Arrays.asList(input.getResult())).size() > 0) {
			topProperties.addAll(result);
			propertyTableViewer.setSelection(new StructuredSelection(result.get(0)));
			updatePropertyButtons();
		}
	}
	
	private void removeProperty() {
		List<String> keys = ((IStructuredSelection)propertyTableViewer.getSelection()).toList();
		if (!keys.isEmpty()) {
			topProperties.removeAll(keys);
			updatePropertyButtons();
		}
	}
	
	private void movePropertyUp() {
		String key = (String) ((IStructuredSelection)propertyTableViewer.getSelection()).getFirstElement();
		int idx = topProperties.indexOf(key);
		if (idx - 1 >= 0) {
			topProperties.move(idx, idx - 1);
			updatePropertyButtons();
		}
	}
	
	private void movePropertyDown() {
		String key = (String) ((IStructuredSelection)propertyTableViewer.getSelection()).getFirstElement();
		int idx = topProperties.indexOf(key);
		if (idx >= 0 && idx + 1 < topProperties.size()) {
			topProperties.move(idx, idx + 1);
			updatePropertyButtons();
		}
	}
	
	private void updatePropertyButtons() {
		IStructuredSelection selection = (IStructuredSelection)propertyTableViewer.getSelection();
		String key = (String)selection.getFirstElement();
		int idx = (selection.size() == 1) ? topProperties.indexOf(key) : -1;
		removeButton.setEnabled(key != null);
		upButton.setEnabled(idx - 1 >= 0);
		downButton.setEnabled(idx >= 0 && idx + 1 < topProperties.size());
	}
	
	
	private void load() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		List<String> names = Prefs.toNameList(preferenceStore.getString(Prefs.CRC_TABLE_FAVORITES_NAMES));
		
		topProperties.clear();
		topProperties.addAll(names);
		updatePropertyButtons();
	}
	
	
	@Override
	protected void performDefaults() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		List<String> names = Prefs.toNameList(preferenceStore.getDefaultString(Prefs.CRC_TABLE_FAVORITES_NAMES));
		
		topProperties.clear();
		topProperties.addAll(names);
		updatePropertyButtons();
		
		super.performDefaults();
	}
	
	@Override
	public boolean performOk() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.setValue(Prefs.CRC_TABLE_FAVORITES_NAMES, String.join(",", topProperties));
		return true;
	}
	
}
