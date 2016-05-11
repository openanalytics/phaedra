package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.MessageDialog;
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
}
