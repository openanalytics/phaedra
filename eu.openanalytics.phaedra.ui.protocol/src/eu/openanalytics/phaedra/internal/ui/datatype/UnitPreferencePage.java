package eu.openanalytics.phaedra.internal.ui.datatype;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class UnitPreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	
	
	private final WritableValue<ConcentrationUnit> concentrationUnit;
	private final WritableValue<ConcentrationUnit> curveConcentrationUnit;
	
	private ComboViewer concentrationUnitViewer;
	private ComboViewer curveConcentrationUnitViewer;
	
	
	public UnitPreferencePage() {
		this.concentrationUnit = new WritableValue<>();
		this.curveConcentrationUnit = new WritableValue<>();
	}
	
	@Override
	public void init(final IWorkbench workbench) {
	}
	
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return DataTypePrefs.getPreferenceStore();
	}
	
	
	@Override
	protected Control createContents(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		final Composite groupComposite = createGroup(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(groupComposite);
		
		initDatabinding();
		load();
		
		return composite;
	}
	
	protected Composite createGroup(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Concentration unit of well compounds:");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			
			final ComboViewer viewer = new ComboViewer(composite, SWT.BORDER | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					final ConcentrationUnit unit = (ConcentrationUnit)element;
					return unit.getLabel(true);
				}
			});
			viewer.setInput(ConcentrationUnit.values());
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
					.applyTo(viewer.getControl());
			this.concentrationUnitViewer = viewer;
		}
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Concentration unit of curve properties:");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			
			final ComboViewer viewer = new ComboViewer(composite, SWT.BORDER | SWT.READ_ONLY);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					final ConcentrationUnit unit = (ConcentrationUnit)element;
					return unit.getLabel(true);
				}
			});
			viewer.setInput(ConcentrationUnit.values());
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
					.applyTo(viewer.getControl());
			this.curveConcentrationUnitViewer = viewer;
		}
		
		return composite;
	}
	
	private void initDatabinding() {
		final DataBindingContext dbc = new DataBindingContext();
		dbc.bindValue(ViewerProperties.singleSelection().observe(this.concentrationUnitViewer),
				this.concentrationUnit );
		dbc.bindValue(ViewerProperties.singleSelection().observe(this.curveConcentrationUnitViewer),
				this.curveConcentrationUnit );
	}
	
	
	private void load() {
		final IPreferenceStore preferenceStore = getPreferenceStore();
		this.concentrationUnit.setValue(ConcentrationUnit.valueOf(preferenceStore.getString(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT)));
		this.curveConcentrationUnit.setValue(ConcentrationUnit.valueOf(preferenceStore.getString(DataTypePrefs.CURVE_CONCENTRATION_UNIT)));
	}
	
	
	@Override
	protected void performDefaults() {
		final IPreferenceStore preferenceStore = getPreferenceStore();
		this.concentrationUnit.setValue(ConcentrationUnit.valueOf(preferenceStore.getDefaultString(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT)));
		this.curveConcentrationUnit.setValue(ConcentrationUnit.valueOf(preferenceStore.getDefaultString(DataTypePrefs.CURVE_CONCENTRATION_UNIT)));
		
		super.performDefaults();
	}
	
	@Override
	public boolean performOk() {
		final IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.setValue(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT, this.concentrationUnit.getValue().name());
		preferenceStore.setValue(DataTypePrefs.CURVE_CONCENTRATION_UNIT, this.curveConcentrationUnit.getValue().name());
		return true;
	}
	
}
