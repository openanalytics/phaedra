package eu.openanalytics.phaedra.ui.link.importer.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.importer.Activator;
import eu.openanalytics.phaedra.link.importer.preferences.Prefs;

public class ImportWizard extends Wizard {

	private SelectionPage selectionPage;
	
	private IWizardState preconfiguredState;
	
	public ImportWizard(IWizardState state) {
		setWindowTitle("Import Wizard");
		setForcePreviousAndNextButtons(true);
		this.preconfiguredState = state;
	}
	
	@Override
	public void addPages() {
		addPage(selectionPage = new SelectionPage());
		// The rest of the pages are added dynamically by SelectionPage.
	}
	
	@Override
	public boolean performFinish() {
		return true;
	}
	
	public void launchSpecifiedWizard(final WizardDescriptor wizardToSelect) {
		IWizardNode node = new WizardNode(wizardToSelect);
		selectionPage.setSelectedNode(node);
		selectionPage.getWizard().getContainer().showPage(selectionPage.getNextPage());
	}

	private class SelectionPage extends WizardSelectionPage {
       
		SelectionPage() {
            super("Select Import Wizard");
        }

        public void createControl(Composite parent) {
        	Composite container = new Composite(parent, SWT.NONE);
        	GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
        	
        	Composite buttonContainer = new Composite(container, SWT.NONE);
        	GridDataFactory.fillDefaults().indent(20, 20).grab(true,true).align(SWT.CENTER, SWT.BEGINNING).applyTo(buttonContainer);
        	GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonContainer);
        	
        	WizardDescriptor[] wizards = ImportWizardRegistry.getInstance().getImportWizards();
        	for (WizardDescriptor wizard: wizards) {
        		final WizardDescriptor wizardToSelect = wizard;
        		Button b = new Button(buttonContainer, SWT.PUSH);
        		if (wizard.getIcon() != null) {
        			b.setImage((Image)JFaceResources.getResources().get(wizard.getIcon()));
        		}
        		b.setText(wizard.getName());
        		b.addSelectionListener(new SelectionAdapter() {
        			@Override
        			public void widgetSelected(SelectionEvent e) {
        				launchSpecifiedWizard(wizardToSelect);
        			}
				});
        		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(b);
        		
        		Label l = new Label(buttonContainer, SWT.WRAP);
        		if (wizard.getDescription() != null) l.setText(wizard.getDescription());
        		GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(l);
        	}
        	
        	Group extraButtonContainer = new Group(container, SWT.NONE);
        	extraButtonContainer.setText("Additional options");
        	GridDataFactory.fillDefaults().grab(true,false).align(SWT.FILL, SWT.END).applyTo(extraButtonContainer);
        	GridLayoutFactory.fillDefaults().numColumns(1).margins(5,  5).spacing(5, 5).applyTo(extraButtonContainer);
        	
        	Button createWellFeaturesBtn = new Button(extraButtonContainer, SWT.CHECK);
        	createWellFeaturesBtn.setText("Prompt to create undefined well features");
        	createWellFeaturesBtn.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_WELL_FEATURES));
        	createWellFeaturesBtn.addSelectionListener(new SelectionAdapter() {
        		@Override
        		public void widgetSelected(SelectionEvent e) {
        			Activator.getDefault().getPreferenceStore().setValue(Prefs.DETECT_WELL_FEATURES, createWellFeaturesBtn.getSelection());
        		}
			});
        	Button createSubWellFeaturesBtn = new Button(extraButtonContainer, SWT.CHECK);
        	createSubWellFeaturesBtn.setText("Prompt to create undefined sub-well features");
        	createSubWellFeaturesBtn.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_SUBWELL_FEATURES));
        	createSubWellFeaturesBtn.addSelectionListener(new SelectionAdapter() {
        		@Override
        		public void widgetSelected(SelectionEvent e) {
        			Activator.getDefault().getPreferenceStore().setValue(Prefs.DETECT_SUBWELL_FEATURES, createSubWellFeaturesBtn.getSelection());
        		}
			});
        		
        	setTitle("Select Wizard");
        	setDescription("Select one of the available wizards below."
        			+ "\nIf an appropriate wizard is not listed, use the Generic Importer.");
        	setControl(container);
        }
        
        @Override
        protected void setSelectedNode(IWizardNode node) {
        	// Make method visible to ImportWizard.
        	super.setSelectedNode(node);
        }
    }
	
	private class WizardNode implements IWizardNode {
		
		private WizardDescriptor wizardDescriptor;
		private IWizard wizard;
		
		public WizardNode(WizardDescriptor wizardDescriptor) {
			this.wizardDescriptor = wizardDescriptor;
		}
		
		@Override
		public boolean isContentCreated() {
			return wizard != null;
		}
		
		@Override
		public IWizard getWizard() {
			if (wizard == null) {
				try {
					wizard = wizardDescriptor.createWizard();
				} catch (CoreException e) {
					throw new RuntimeException("Failed to load import wizard '" + wizardDescriptor.getName() + "': " + e.getMessage(), e);
				}
			}
			if (wizard instanceof BaseStatefulWizard && preconfiguredState != null) {
				((BaseStatefulWizard)wizard).setPreconfiguredState(preconfiguredState);
			}
			return wizard;
		}
		
		@Override
		public Point getExtent() {
			return new Point(-1, -1);
		}
		
		@Override
		public void dispose() {
			// Nothing to dispose.
		}
	}
}
