package eu.openanalytics.phaedra.ui.project.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.util.dialog.TitleAreaDataBindingDialog;
import eu.openanalytics.phaedra.project.vo.Project;


public class EditProjectDialog extends TitleAreaDataBindingDialog {

	private Project project;

	private Text nameControl;
	private Text descriptionControl;

	private Text ownerControl;
	private ComboViewer teamViewer;
	private ComboViewer accessViewer;

	private boolean canEdit;
	private boolean canDelete;
	private List<String> validTeams;


	public EditProjectDialog(Shell parentShell, Project project) {
		super(parentShell);
		
		this.project = project;
		
		this.canEdit = SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, project);
		this.canDelete = canEdit && SecurityService.getInstance().checkPersonalObject(SecurityService.Action.DELETE, project);
		this.validTeams = getValidTeams();
		
		setDialogTitle((project.getId() == 0) ? "Create Project" : "Edit Project");
		if (canEdit) {
			setDialogMessage("Configure the properties of the project.");
		} else {
			setValidationEnabled(false);
			setDialogMessage("You do not have permission to edit the properties of the project.", IMessageProvider.INFORMATION);
		}
	}

	protected List<String> getValidTeams() {
		List<String> teams = new ArrayList<String>();
		teams.add("NONE");
		teams.addAll(SecurityService.getInstance().getAccessibleTeams());
		return teams;
	}


	@Override
	public void create() {
		super.create();
		
		if (project.getId() == 0) {
			getShell().getDisplay().asyncExec(() -> {
				if (nameControl.isDisposed()) return;
				nameControl.selectAll();
			});
		}
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite dialogArea = super.createDialogAreaComposite(parent);
		
		Composite content = createEditProject(dialogArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(content);
		
		return dialogArea;
	}

	private Composite createEditProject(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		
		{	Label label = new Label(container, SWT.NONE);
			label.setText("Name:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			
			Text text = new Text(container, SWT.BORDER);
			text.setTextLimit(100);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(text);
			nameControl = text;
			nameControl.setEditable(canEdit);
		}
		{	Label label = new Label(container, SWT.NONE);
			label.setText("Description / Notes:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
			
			Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			text.setTextLimit(100);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).hint(SWT.DEFAULT, convertHeightInCharsToPixels(4)).grab(true, true).applyTo(text);
			descriptionControl = text;
			descriptionControl.setEditable(canEdit);
		}
		
		Composite securityProperties = createSecurityProperties(container);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(securityProperties);
		
		return container;
	}

	private Composite createSecurityProperties(Composite parent) {
		Group container = new Group(parent, SWT.NONE);
		container.setText("&Permissions:");
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(container);
		
		{	Label label = new Label(container, SWT.NONE);
			label.setText("Owner:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			
			Text text = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(text);
			ownerControl = text;
			ownerControl.setEditable(false);
		}
		{	Label label = new Label(container, SWT.NONE);
			label.setText("Sharing:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			
			ComboViewer viewer = new ComboViewer(container, SWT.READ_ONLY | SWT.BORDER);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((AccessScope)element).getName();
				}
			});
			viewer.setInput(AccessScope.values());
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(viewer.getControl());
			accessViewer = viewer;
			accessViewer.getControl().setEnabled(canDelete);
		}
		{	Label label = new Label(container, SWT.NONE);
			label.setText("Team:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			
			ComboViewer viewer = new ComboViewer(container, SWT.READ_ONLY | SWT.BORDER);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new LabelProvider());
			viewer.setInput(validTeams);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(viewer.getControl());
			teamViewer = viewer;
			teamViewer.getControl().setEnabled(canDelete);
		}
		
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (canEdit) {
			super.createButtonsForButtonBar(parent);
		}
		else {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		}
	}

	@Override
	protected void initDataBinding(DataBindingContext dbc) {
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(nameControl),
				PojoProperties.value("name", String.class).observe(project), //$NON-NLS-1$
				new UpdateValueStrategy().setAfterGetValidator((value) -> {
					String s = (String) value;
					if (s.isEmpty()) {
						return ValidationStatus.error("Enter a name for the project.");
					}
					return ValidationStatus.ok();
				}),
				null);
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(descriptionControl),
				PojoProperties.value("description", String.class).observe(project)); //$NON-NLS-1$
		
		dbc.bindValue(WidgetProperties.text().observe(ownerControl),
				PojoProperties.value("owner", String.class).observe(project)); //$NON-NLS-1$
		IViewerObservableValue accessTargetObs = ViewerProperties.singleSelection().observe(accessViewer);
		dbc.bindValue(accessTargetObs, PojoProperties.value("accessScope", AccessScope.class).observe(project)); //$NON-NLS-1$
		IViewerObservableValue teamTargetObs = ViewerProperties.singleSelection().observe(teamViewer);
		dbc.bindValue(teamTargetObs, PojoProperties.value("teamCode", String.class).observe(project)); //$NON-NLS-1$
		dbc.addValidationStatusProvider(new MultiValidator() {
			@Override
			protected IStatus validate() {
				AccessScope accessScope = (AccessScope)accessTargetObs.getValue();
				String team = (String)teamTargetObs.getValue();
				if (accessScope == AccessScope.TEAM
						&& (team == null || team.equals("NONE"))) {
					return ValidationStatus.warning("Select a team to make the project accessible to team members.");
				}
				return ValidationStatus.ok();
			}
		});
	}

}
