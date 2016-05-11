package eu.openanalytics.phaedra.base.console;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class InteractiveConsole {

	private final static String LINE_SEP = System.getProperty("line.separator");
	
	private IOConsole console;
	private IDocumentListener inputListener;
	private IOConsoleOutputStream output;
	private IOConsoleOutputStream outputErr;
	private PrintWriter outputErrWriter;
	
	private ImageDescriptor icon;
	
	public InteractiveConsole(String name, ImageDescriptor icon) {
		this.console = new IOConsole(name, null);
		this.icon = icon;
		
		IConsoleManager consoleMgr = ConsolePlugin.getDefault().getConsoleManager();
		consoleMgr.addConsoles(new IConsole[] { console });

		createInputListener();
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				console.getInputStream().setColor(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
				console.getDocument().addDocumentListener(inputListener);
				output = console.newOutputStream();
				outputErr = console.newOutputStream();
				outputErr.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				outputErrWriter = new PrintWriter(outputErr);
			}
		});
	}
	
	public String getName() {
		return console.getName();
	}
	
	public ImageDescriptor getIcon() {
		return icon;
	}
	
	public void bringToTop() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow != null) {
			IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
			if (activePage != null) {
				IConsoleView v = (IConsoleView)activePage.findView(IConsoleConstants.ID_CONSOLE_VIEW);
				if (v != null) v.display(console);
			}
		}
	}
	
	public void print(String message) {
		try {
			if (PlatformUI.isWorkbenchRunning()) output.write(message + LINE_SEP);
        } catch (IOException e) {
            ConsolePlugin.log(e);
        }
	}
	
	public void printErr(String message) {
		try {
			if (PlatformUI.isWorkbenchRunning()) outputErr.write(message + LINE_SEP);
        } catch (IOException e) {
            ConsolePlugin.log(e);
        }
	}
	
	public void printErr(String message, Throwable cause) {
		if (PlatformUI.isWorkbenchRunning()) {
			printErr(message);
			cause.printStackTrace(outputErrWriter);
			outputErrWriter.flush();
		}
	}
	
	protected String processInput(String input) throws Exception {
		// Default behaviour: do nothing.
		return null;
	}
	
	private void createInputListener() {
		inputListener = new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// Do nothing.
			}
			
			@Override
			public void documentChanged(DocumentEvent event) {
				String text = event.getText();
				if (text.equals(LINE_SEP)) {
					String input = null;
					try {
						int lineCount = event.getDocument().getNumberOfLines();
						IRegion region = event.getDocument().getLineInformation(lineCount-2);
						input = event.getDocument().get(region.getOffset(), region.getLength());
						
						String output = null;
						if (input != null && !input.isEmpty()) {
							try {
								output = processInput(input);
							} catch (Throwable t) {
								printErr(t.getMessage());
							}
						}
						if (output != null && !output.isEmpty()) {
							print(output);
						}
					} catch (Exception e) {
						 ConsolePlugin.log(e);
					}
				}
			}
		};
	}
}
