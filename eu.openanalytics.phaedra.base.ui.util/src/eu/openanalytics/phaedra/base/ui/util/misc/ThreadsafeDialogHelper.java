package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Convenience class that opens message dialogs and waits for them to close
 * regardless of which thread makes the call. 
 */
public class ThreadsafeDialogHelper {

	public static boolean openQuestion(final String title, final String message) {
		final AtomicBoolean b = new AtomicBoolean(true);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				boolean outcome = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), title, message);
				b.set(outcome);
			}
		});
		return b.get();
	}
	
	public static boolean openConfirm(final String title, final String message) {
		final AtomicBoolean b = new AtomicBoolean(true);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				boolean outcome = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), title, message);
				b.set(outcome);
			}
		});
		return b.get();
	}
	
	public static void openWarning(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openWarning(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
	public static void openError(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
	public static void openInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
	public static String openInput(final String title, final String message, final String initValue) {
		final StringBuilder sb = new StringBuilder();
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), title, message, initValue, null);
				int retCode = dialog.open();
				if (retCode == Window.OK) sb.append(dialog.getValue());
			}
		});
		return sb.toString();
	}
	
	public static String openFolderSelector(final String message, final String selectedPath) {
		final StringBuilder sb = new StringBuilder();
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
				dialog.setMessage(message);
				if (selectedPath != null) dialog.setFilterPath(selectedPath);
				String retVal = dialog.open();
				if (retVal != null) sb.append(retVal);
			}
		});
		return sb.toString();
	}
}
