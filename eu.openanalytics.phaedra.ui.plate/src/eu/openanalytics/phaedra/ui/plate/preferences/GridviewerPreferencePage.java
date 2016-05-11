package eu.openanalytics.phaedra.ui.plate.preferences;

import static eu.openanalytics.phaedra.ui.plate.preferences.Prefs.SHOW_DEFAULT;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeaturePlateLayer;


public class GridviewerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public GridviewerPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		Label lbl = new Label(getFieldEditorParent(), SWT.NONE);
		lbl.setText("The layers selected below will be enabled by default:");

		Group group = createGroup(getFieldEditorParent(), "Plate Grid Layers");
		Composite plateLayer = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(plateLayer);
		GridLayoutFactory.fillDefaults().applyTo(plateLayer);

		group = createGroup(getFieldEditorParent(), "Feature Correlation Grid Layers");
		Composite featureCorrComp = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(featureCorrComp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(featureCorrComp);

		group = createGroup(featureCorrComp, "Well");
		Composite wellFeatureCorr = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(wellFeatureCorr);
		GridLayoutFactory.fillDefaults().applyTo(wellFeatureCorr);

		group = createGroup(featureCorrComp, "Subwell");
		Composite subwellFeatureCorr = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subwellFeatureCorr);
		GridLayoutFactory.fillDefaults().applyTo(subwellFeatureCorr);

		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IGridLayer.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IGridLayer.ATTR_CLASS);
				if (o instanceof IGridLayer) {
					IGridLayer layer = (IGridLayer) o;
					if (layer instanceof BaseGridLayer) {
						BaseGridLayer baseLayer = (BaseGridLayer) layer;
						if (baseLayer instanceof PlatesLayer) {
							addField(new BooleanFieldEditor(SHOW_DEFAULT + baseLayer.getClass(),
									baseLayer.getName(),
									plateLayer));
						} else if (baseLayer instanceof FeaturePlateLayer) {
							addField(new BooleanFieldEditor(SHOW_DEFAULT + baseLayer.getClass(),
									baseLayer.getName(),
									wellFeatureCorr));
						} else {
							addField(new BooleanFieldEditor(SHOW_DEFAULT + baseLayer.getClass(),
									baseLayer.getName(),
									subwellFeatureCorr));
						}
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}

	private Group createGroup(Composite parent, String title) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		return group;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		Button resetButton = new Button(parent, SWT.NONE);
		resetButton.setText("Reset to preferences");
		resetButton.addListener(SWT.MouseDown, e -> {
			IConfigurationElement[] config = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(IGridLayer.EXT_PT_ID);
			for (IConfigurationElement el : config) {
				try {
					Object o = el.createExecutableExtension(IGridLayer.ATTR_CLASS);
					if (o instanceof IGridLayer) {
						IGridLayer layer = (IGridLayer) o;
						if (layer instanceof BaseGridLayer) {
							GridState.removeValue(GridState.ALL_PROTOCOLS, el.getAttribute(IGridLayer.ATTR_ID), GridState.DEFAULT_ENABLED);
						}
					}
				} catch (CoreException ce) {
					// Invalid extension.
				}
			}
		});
		resetButton.setOrientation(SWT.LEFT_TO_RIGHT);

		return control;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
