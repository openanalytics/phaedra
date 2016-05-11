package eu.openanalytics.phaedra.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorSupportProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import eu.openanalytics.phaedra.base.email.MailService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;

public class CustomErrorSupportProvider extends ErrorSupportProvider {

	private static final String SUPPORT_LIST_NAME = "phaedra.support";

	@Override
	public Control createSupportArea(Composite parent, final IStatus status) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Support:");

		final Link link = new Link(container, SWT.NONE);
		link.setText("<a>Mail this error to Phaedra support</a>");
		link.addListener (SWT.Selection, new Listener () {
			@Override
			public void handleEvent(Event event) {
				doMailError(status, true);
			}
		});

		return container;
	}

	private static void doMailError(IStatus status, boolean userPermission) {

		String info = "";
		if (userPermission) {
			// Ask for additional input.
			InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(),
					"Additional Information","Additional information to include in the report:",info,null);
			int retCode = dialog.open();
			if (retCode == Window.CANCEL) return;

			info = dialog.getValue();
		}
		String userName = SecurityService.getInstance().getCurrentUserName();

		String sender = "phaedra-client@" + MailService.getInstance().getMailSuffix();
		String subject = "Phaedra Error Report (" + userName + ")";

		try {
			// Create text message.
			StringBuilder body = new StringBuilder();
			body.append("User: " + userName + "\n");
			InetAddress address = InetAddress.getLocalHost();
			String hostName = address.getHostName();
			body.append("Pc: " + hostName + "\n");
			body.append("\n");
			body.append("Phaedra location: " + new File(".").getAbsolutePath() + "\n");
			body.append("Phaedra version: " + VersionUtils.getPhaedraVersion() + "\n");
			body.append("\n");
			body.append("Message: " + status.getMessage() + "\n");
			body.append("Source: " + status.getPlugin() + "\n");
			body.append("Timestamp: " + new Date() + "\n");
			body.append("\n");
			body.append("Additional info: " + info + "\n");
			body.append("\n");
			if (status.getException() != null) {
				body.append("Error Message: " + status.getException().getMessage() + "\n");
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintWriter writer = new PrintWriter(out);
				status.getException().printStackTrace(writer);
				writer.flush();
				writer.close();
				String stack = out.toString();
				body.append("Error Stacktrace:" + "\n");
				body.append(stack);
			}

			URL[] attachments = { Platform.getLogFileLocation().toFile().toURI().toURL() };
			MailService.getInstance().sendMail(sender, SUPPORT_LIST_NAME, subject, body.toString(), attachments);

			if (userPermission) {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Mail Sent",
						"The report was sent to the Phaedra support team.");
			}
		} catch (Exception ex) {
			if (userPermission) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Failed to send report",
						"The error report could not be sent. Please contact Phaedra support directly");
			}
		}
	}
}
