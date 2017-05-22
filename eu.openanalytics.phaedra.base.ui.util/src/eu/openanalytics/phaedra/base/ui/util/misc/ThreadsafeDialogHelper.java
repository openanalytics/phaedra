package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
	
	public static int openChoice(final String title, final String message, String[] choices) {
		AtomicInteger retVal = new AtomicInteger();
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				ChoiceDialog dialog = new ChoiceDialog(Display.getCurrent().getActiveShell(), title, message, choices);
				dialog.open();
				retVal.set(dialog.getSelectedChoice());
			}
		});
		return retVal.get();
	}
	
	private static class ChoiceDialog extends TitleAreaDialog {

		private String title;
		private String msg;
		private String[] choices;
		
		private Button[] choiceButtons;
		private int selectedChoice;
		
		public ChoiceDialog(Shell parentShell, String title, String msg, String[] choices) {
			super(parentShell);
			this.title = title;
			this.msg = msg;
			this.choices = choices;
			this.selectedChoice = -1;
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(title);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			setTitle(title);
			setMessage(msg);

			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

			choiceButtons = new Button[choices.length];
			for (int i = 0; i < choiceButtons.length; i++) {
				choiceButtons[i] = new Button(container, SWT.RADIO);
				choiceButtons[i].setText(choices[i]);
			}

			return area;
		}
		
		@Override
		protected void okPressed() {
			for (int i = 0; i < choiceButtons.length; i++) {
				if (choiceButtons[i].getSelection()) selectedChoice = i;
			}
			super.okPressed();
		}
		
		public int getSelectedChoice() {
			return selectedChoice;
		}
	}
}
