package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

public class ContextHelper {

	/**
	 * Attach a context to a control. Contexts are used for things like key bindings (CTRL+A, CTRL+C, etc)
	 * <ul>
	 * <li>Whenever the control receives focus, the given context is activated.</li>
	 * <li>When the control loses focus, the context is deactivated.</li>
	 * </ul>
	 * 
	 * @param control The control to associate the context to.
	 * @param contextId The context to associate with the control.
	 */
	public static void attachContext(Control control, String contextId) {
		if (control == null || contextId == null) return;
		
		IContextService service = (IContextService)PlatformUI.getWorkbench().getService(IContextService.class);
		control.addFocusListener(new ContextSwitcher(contextId, service));
	}
	
	private static class ContextSwitcher implements FocusListener {
		
		private String contextId;
		private IContextService service;
		private IContextActivation activation = null;
		
		public ContextSwitcher(String contextId, IContextService service) {
			this.contextId = contextId;
			this.service = service;
		}
		
		@Override
		public void focusGained(FocusEvent e) {
			activation = service.activateContext(contextId);	
		}
		
		@Override
		public void focusLost(FocusEvent e) {
			if (activation != null) service.deactivateContext(activation);
		}
	}
}
