package eu.openanalytics.phaedra.base.ui.util.view;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.WorkbenchPart;

public class PartDecorator {

	private WorkbenchPart workBenchPart;

	public void onCreate(Composite parent) {
		// Default behaviour: do nothing.
	}

	public void onDispose() {
		// Default behaviour: do nothing.
	}

	public void contributeContextMenu(IMenuManager manager) {
		// Default behaviour: do nothing.
	}

	public void contributeToolbar(IToolBarManager manager) {
		// Default behaviour: do nothing.
	}

	/* package */ void setWorkBenchPart(WorkbenchPart workBenchPart) {
		this.workBenchPart = workBenchPart;
	}

	protected WorkbenchPart getWorkBenchPart() {
		return workBenchPart;
	}

}
