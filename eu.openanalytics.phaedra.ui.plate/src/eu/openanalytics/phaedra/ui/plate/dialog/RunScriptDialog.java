package eu.openanalytics.phaedra.ui.plate.dialog;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class RunScriptDialog extends TitleAreaDialog {

	private List<Plate> plates;

	private Combo scriptCmb;
	private Text argsTxt;

	public RunScriptDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Run Script");
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		// main of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		// main of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		new Label(main, SWT.NONE).setText("Plates:");
		new Label(main, SWT.NONE).setText(String.format("%d selected", plates.size()));

		new Label(main, SWT.NONE).setText("Script:");

		scriptCmb = new Combo(main, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scriptCmb);

		new Label(main, SWT.NONE).setText("Arguments:");

		argsTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(argsTxt);

		setTitle("Run Script");
		setMessage("Run a script on a selection of plates.\nSelect a script to run in the list below.");

		try {
			List<String> scripts = ScriptService.getInstance().getCatalog().getAvailableScripts("misc");
			scriptCmb.setItems(scripts.toArray(new String[scripts.size()]));
			if (!scripts.isEmpty()) scriptCmb.select(0);
		} catch (IOException e) {
			EclipseLog.error("Failed to load script catalog", e, Activator.getDefault());
		}

		return main;
	}

	@Override
	protected void okPressed() {
		String script = scriptCmb.getText();
		String[] args = argsTxt.getText().split(",");

		Job job = new Job("Running script " + script) {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(String.format("Running script '%s' on %d plate(s)", script, plates.size()), plates.size());
				for (Plate plate: plates) {
					try {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						Object[] scriptArgs = new Object[args.length + 1];
						scriptArgs[0] = plate.getId();
						for (int i = 0; i < args.length; i++) scriptArgs[i+1] = args[i];
						ScriptService.getInstance().getCatalog().run(script, scriptArgs);
					} catch (Exception e) {
						String msg = String.format("Failed to run script '%s' on %s", script, plate.toString());
						return new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e);
					}
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		super.okPressed();
	}
}
