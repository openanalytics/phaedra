package eu.openanalytics.phaedra.ui.curve.prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataDescriptionUtils;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider.CurveTextField;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.ui.curve.Activator;
import eu.openanalytics.phaedra.ui.curve.details.CrcDetailsView;


public class PropertyPreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	
	
	private class PropertyLabelProvider extends ColumnLabelProvider {
		
		@Override
		public String getText(final Object element) {
			final String name = (String)element;
			final DataDescription dataDescription = availableProperties.get(name);
			return (dataDescription != null) ? dataDescription.convertNameTo(name, dataFormatSupport.get()) : name;
		}
		
	}
	
	
	private final DataFormatSupport dataFormatSupport;
	
	private final HashMap<String, DataDescription> availableProperties;
	private final List<DataDescription> availableDefaultProperties;
	private final List<DataDescription> availableModelProperties;
	
	private final WritableList<String> topProperties;
	
	private TableViewer propertyTableViewer;
	private Button addButton;
	private Button removeButton;
	private Button upButton;
	private Button downButton;
	
	
	public PropertyPreferencePage() {
		this.dataFormatSupport = new DataFormatSupport(this::refreshTable);
		
		this.topProperties = new WritableList<>();
		
		this.availableProperties = new HashMap<>();
		this.availableDefaultProperties = new ArrayList<>();
		this.availableModelProperties = new ArrayList<>();
		loadAvailableProperties();
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}
	
	@Override
	public void dispose() {
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
		super.dispose();
	}
	
	
	private void loadAvailableProperties() {
		for (CurveTextField column : CurveTextProvider.getColumns(null, this.dataFormatSupport.get())) {
			final DataDescription dataDescription = column.getDataDescription();
			this.availableDefaultProperties.add(dataDescription);
			this.availableProperties.put(dataDescription.getName(), dataDescription);
		}
		for (String modelId : CurveFitService.getInstance().getFitModels()) {
			for (CurveParameter.Definition def : CurveFitService.getInstance().getModel(modelId).getOutputParameters(null)) {
				final DataDescription dataDescription = def.getDataDescription();
				if (!this.availableProperties.containsKey(dataDescription.getName())) {
					this.availableProperties.put(dataDescription.getName(), dataDescription);
					this.availableModelProperties.add(dataDescription);
				}
			}
		}
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		Curve curve = (part instanceof CrcDetailsView) ? ((CrcDetailsView) part).getCurve() : null;
		if (curve != null) {
			for (Value value : curve.getOutputParameters()) {
				final DataDescription dataDescription = value.definition.getDataDescription();
				if (!this.availableProperties.containsKey(dataDescription.getName())) {
					this.availableProperties.put(dataDescription.getName(), dataDescription);
					this.availableModelProperties.add(dataDescription);
				}
			}
		}
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
	
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
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
		
		Composite tableComposite = new Composite(composite, SWT.NONE);
		TableColumnLayout columnLayout = new TableColumnLayout();
		TableViewer viewer = new TableViewer(tableComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
		viewer.setContentProvider(new ObservableListContentProvider());
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.setLabelProvider(new PropertyLabelProvider());
		columnLayout.setColumnData(column.getColumn(), new ColumnWeightData(100));
		
		viewer.setInput(this.topProperties);
		tableComposite.setLayout(columnLayout);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(tableComposite);
		
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
		
		addButton = addButton(buttons, "Add Property...", this::addProperty);
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
		List<String> names = new ArrayList<>(this.availableDefaultProperties.size() + this.availableModelProperties.size());
		for (final DataDescription dataDescription : this.availableDefaultProperties) {
			if (!this.topProperties.contains(dataDescription.getName())) {
				names.add(dataDescription.getName());
			}
		}
		this.availableModelProperties.sort(DataDescriptionUtils.getNameComparator(this.dataFormatSupport.get()));
		for (final DataDescription dataDescription : this.availableModelProperties) {
			if (!this.topProperties.contains(dataDescription.getName())) {
				names.add(dataDescription.getName());
			}
		}
		
		ListSelectionDialog input = new ListSelectionDialog(getShell(), names, new ArrayContentProvider(), new PropertyLabelProvider(), null);
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
		List<String> names = ((IStructuredSelection)propertyTableViewer.getSelection()).toList();
		if (!names.isEmpty()) {
			topProperties.removeAll(names);
			updatePropertyButtons();
		}
	}
	
	private void movePropertyUp() {
		String name = (String) ((IStructuredSelection)propertyTableViewer.getSelection()).getFirstElement();
		int idx = topProperties.indexOf(name);
		if (idx - 1 >= 0) {
			topProperties.move(idx, idx - 1);
			updatePropertyButtons();
		}
	}
	
	private void movePropertyDown() {
		String name = (String) ((IStructuredSelection)propertyTableViewer.getSelection()).getFirstElement();
		int idx = topProperties.indexOf(name);
		if (idx >= 0 && idx + 1 < topProperties.size()) {
			topProperties.move(idx, idx + 1);
			updatePropertyButtons();
		}
	}
	
	private void refreshTable() {
		if (this.propertyTableViewer == null || this.propertyTableViewer.getControl().isDisposed()) {
			return;
		}
		this.propertyTableViewer.refresh();
	}
	
	private void updatePropertyButtons() {
		IStructuredSelection selection = (IStructuredSelection)propertyTableViewer.getSelection();
		String name = (String)selection.getFirstElement();
		int idx = (selection.size() == 1) ? topProperties.indexOf(name) : -1;
		removeButton.setEnabled(name != null);
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
