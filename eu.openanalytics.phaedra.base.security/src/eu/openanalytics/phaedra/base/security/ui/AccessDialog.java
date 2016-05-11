package eu.openanalytics.phaedra.base.security.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class AccessDialog extends Dialog {
	
	private String title = "Access Denied";
	private String message;
	private String requiredRole = "-";
	private String yourRole = "-";

	private Font smallFont;

	private boolean showRoles = true;

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	/**
	 * Create the dialog
	 * 
	 * @param parentShell
	 */
	public AccessDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setRequiredRole(String requiredRole) {
		this.requiredRole = requiredRole;
	}

	public void setYourRole(String yourRole) {
		this.yourRole = yourRole;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Create contents of the dialog
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 3;
		gridLayout.horizontalSpacing = 10;
		gridLayout.numColumns = 2;
		area.setLayout(gridLayout);
		toolkit.adapt(area);

		final CLabel titleLabel = new CLabel(area, SWT.NONE);
		Font font = new Font(null, "@Dotum", 20, SWT.BOLD);
		titleLabel.setFont(font);
		final GridData gd_titleLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		titleLabel.setLayoutData(gd_titleLabel);
		toolkit.adapt(titleLabel, true, true);
		titleLabel.setText(title);

		final Composite seperator = toolkit.createCompositeSeparator(area);
		final GridData gd_seperator = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_seperator.heightHint = 2;
		seperator.setLayoutData(gd_seperator);

		final Label icon = toolkit.createLabel(area, "new Forms Label", SWT.NONE);
		icon.setImage(IconManager.getIconImage("lock_128.png"));
		final GridData gd_icon = new GridData();
		icon.setLayoutData(gd_icon);

		final Composite placeholder = toolkit.createComposite(area, SWT.NONE);
		final GridData gd_placeholder = new GridData(SWT.FILL, SWT.FILL, true, true);
		placeholder.setLayoutData(gd_placeholder);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 3;
		gridLayout_1.marginWidth = 10;
		gridLayout_1.marginHeight = 10;
		gridLayout_1.horizontalSpacing = 10;
		placeholder.setLayout(gridLayout_1);
		toolkit.paintBordersFor(placeholder);

		final Label descriptionLabel = toolkit.createLabel(placeholder, this.message, SWT.WRAP);
		final GridData gd_descriptionLabel = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		descriptionLabel.setLayoutData(gd_descriptionLabel);

		if (showRoles) {
			final Label iconRequiredRole = toolkit.createLabel(placeholder, "", SWT.NONE);
			iconRequiredRole.setImage(IconManager.getIconImage("lock_red.png"));

			final Label labelPreRequired = toolkit.createLabel(placeholder, "Required role:", SWT.NONE);
			smallFont = new Font(null, "", 9, SWT.BOLD);
			labelPreRequired.setFont(smallFont);

			toolkit.createLabel(placeholder, this.requiredRole, SWT.NONE);

			final Label iconYourRole = toolkit.createLabel(placeholder, "", SWT.NONE);
			iconYourRole.setImage(IconManager.getIconImage("lock_green.png"));

			final Label labelPreYour = toolkit.createLabel(placeholder, "Your role:", SWT.NONE);
			labelPreYour.setFont(smallFont);

			toolkit.createLabel(placeholder, this.yourRole, SWT.NONE);
		}

		//
		return area;
	}

	/**
	 * Create contents of the button bar
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(500, 275);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.title);
	}

	public static void openAccessDeniedDialog(String requiredRole, String yourRole) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		String message = "You do not have the required permission to perform this operation.";
		String title = "Permission denied";

		AccessDialog dialog = new AccessDialog(shell);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setRequiredRole(requiredRole);
		dialog.setYourRole(yourRole);

		dialog.open();
	}

	public static void openLimitedAccessDialog(String requiredRole, String yourRole) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		String message = "You don't have the required permission to edit and save the data. You are only able to view the data.";
		String title = "Limited Access";

		AccessDialog dialog = new AccessDialog(shell);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setRequiredRole(requiredRole);
		dialog.setYourRole(yourRole);

		dialog.open();
	}

	public static void openProtocolClassEditDialog(String requiredRole, String yourRole) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		String message = "You have the required permission for a protocol in this protocol class. You may edit this protocol class, but be careful because your changes will affect all protocols.";
		String title = "Warning";

		AccessDialog dialog = new AccessDialog(shell);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setRequiredRole(requiredRole);
		dialog.setYourRole(yourRole);

		dialog.open();
	}

	public static void openLoginFailedDialog(Shell shell, String title, String message) {

		AccessDialog dialog = new AccessDialog(shell);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setShowRoles(false);

		dialog.open();
	}

	public void setShowRoles(boolean showRoles) {
		this.showRoles = showRoles;
	}

	public boolean isShowRoles() {
		return showRoles;
	}

	@Override
	public boolean close() {
		if (smallFont != null) {
			smallFont.dispose();
		}
		return super.close();
	}
}
