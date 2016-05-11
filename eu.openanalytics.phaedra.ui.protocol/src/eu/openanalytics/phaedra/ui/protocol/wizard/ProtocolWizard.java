package eu.openanalytics.phaedra.ui.protocol.wizard;

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
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;

public class ProtocolWizard extends Wizard {

	private SelectionPage selectionPage;
	
	private IWizardState preconfiguredState;
	
	public ProtocolWizard(IWizardState state) {
		setWindowTitle("Protocol Wizard");
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
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
        	
        	WizardDescriptor[] wizards = ProtocolWizardRegistry.getInstance().getImportWizards();
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
        	
        	setTitle("Select Wizard");
        	setDescription("Select one of the available wizards below.");
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
					throw new RuntimeException("Failed to load wizard '" + wizardDescriptor.getName() + "': " + e.getMessage(), e);
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
