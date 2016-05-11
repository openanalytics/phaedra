package eu.openanalytics.phaedra.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class ScriptedScheduler {

	private final static String SCRIPT_CATALOG_PATH = "/script.catalog";
	private final static String SCRIPTED_JOB_PATH = "/startup";
	
	public static void registerJobs() {
		try {
			if (!Screening.getEnvironment().getFileServer().exists(SCRIPT_CATALOG_PATH + SCRIPTED_JOB_PATH)) {
				EclipseLog.info("No startup scripts to run: " + SCRIPTED_JOB_PATH + " directory not found.", Activator.getDefault());
				return;
			}
			List<String> scripts = Screening.getEnvironment().getFileServer().dir(SCRIPT_CATALOG_PATH + SCRIPTED_JOB_PATH);
			for (String script: scripts) {
				final String id = script;
				Job job = new Job("Startup script: " + id) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							ScriptService.getInstance().getCatalog().run(SCRIPTED_JOB_PATH + "/" + id, new HashMap<>());
						} catch (ScriptException e) {
							EclipseLog.error("Startup script error", e, Activator.getDefault());
						}
						return Status.OK_STATUS;
					}
				};
				job.setUser(false);
				job.schedule();
			}
		} catch (IOException e) {
			EclipseLog.error("Failed to register startup scripts", e, Activator.getDefault());
		}
	}
}
