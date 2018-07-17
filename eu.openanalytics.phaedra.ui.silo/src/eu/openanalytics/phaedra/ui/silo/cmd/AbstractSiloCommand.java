package eu.openanalytics.phaedra.ui.silo.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;

import eu.openanalytics.phaedra.silo.vo.Silo;

public abstract class AbstractSiloCommand extends AbstractHandler {

	private static final String VAR_ACTIVE_SILO = "activeSilo";
	private static final String VAR_ACTIVE_SILO_DATASET = "activeSiloDataset";
	private static final String VAR_ACTIVE_COLUMN = "activeColumns";
	
	protected Silo getActiveSilo(ExecutionEvent event) {
		return (Silo) getContextVar(VAR_ACTIVE_SILO, event);
	}
	
	protected String getActiveSiloDataset(ExecutionEvent event) {
		return (String) getContextVar(VAR_ACTIVE_SILO_DATASET, event);
	}
	
	protected String[] getActiveColumns(ExecutionEvent event) {
		return (String[]) getContextVar(VAR_ACTIVE_COLUMN, event);
	}
	
	public static void setActiveSilo(Silo silo) {
		setContextVar(VAR_ACTIVE_SILO, silo);
	}
	
	public static void setActiveSiloDataset(String group) {
		setContextVar(VAR_ACTIVE_SILO_DATASET, group);
	}
	
	public static void setActiveColumns(String[] columnNames) {
		setContextVar(VAR_ACTIVE_COLUMN, columnNames);
	}
	
	private static void setContextVar(String name, Object value) {
		IEvaluationContext ctx = ((IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class)).getCurrentState();
		if (value == null) ctx.getParent().removeVariable(name);
		else ctx.getParent().addVariable(name, value);
	}
	
	private static Object getContextVar(String name, ExecutionEvent event) {
		IEvaluationContext ctx = (IEvaluationContext)event.getApplicationContext();
		return ctx.getVariable(name);
	}
}
