package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;

public class ToggleBreadcrumbs extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Event trigger = (Event) event.getTrigger();
		ToolItem item = (ToolItem) trigger.widget;
		BreadcrumbFactory.toggleBreadcrumbs(!item.getSelection());
		return null;
	}

}