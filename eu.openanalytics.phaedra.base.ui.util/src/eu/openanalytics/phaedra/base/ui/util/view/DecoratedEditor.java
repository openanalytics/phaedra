package eu.openanalytics.phaedra.base.ui.util.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.EditorPart;

/**
 * <p>An instance of this class is a {@link EditorPart} that can be decorated with additional functionality.
 * The additional functionality must be provided in the form of ViewDecorator objects.
 * </p><p>
 * <i>Note:</i> the decorators must be added via addDecorator(ViewDecorator decorator) during the execution of
 * createPartControl(Composite parent), <b>before</b> calling super.createPartControl(Composite parent) !
 * </p>
 */
public abstract class DecoratedEditor extends EditorPart implements IDecoratedPart {

	private List<PartDecorator> decorators;
	private MenuManager contextMenu;

	public DecoratedEditor() {
		super();
		decorators = new ArrayList<>();
	}

	@Override
	public void addDecorator(PartDecorator decorator) {
		decorator.setWorkBenchPart(this);
		decorators.add(decorator);
	}

	@Override
	public void removeDecorator(PartDecorator decorator) {
		if (decorators.contains(decorator)) {
			decorator.setWorkBenchPart(null);
			decorators.remove(decorator);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends PartDecorator> E hasDecorator(Class<E> decoratorClass) {
		for (PartDecorator decorator: decorators) {
			if (decoratorClass.isAssignableFrom(decorator.getClass())) return (E) decorator;
		}
		return null;
	}

	@Override
	public void initDecorators(Composite parent, Control... ctxMenuControl) {

		List<PartDecorator> contributedDecorators = DecoratorRegistry.getDecoratorsFor(this);
		for (PartDecorator decorator : contributedDecorators) addDecorator(decorator);

		for (PartDecorator decorator: decorators) {
			decorator.onCreate(parent);
		}

		if (getSite() != null) {
			for (Control control : ctxMenuControl) {
				createContextMenu(control);
			}
			fillToolbar();
		}
	}

	@Override
	public void dispose() {
		for (PartDecorator decorator: decorators) {
			decorator.onDispose();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		for (PartDecorator decorator: decorators) {
			if (adapter.isAssignableFrom(decorator.getClass())) {
				return decorator;
			}
		}
		return super.getAdapter(adapter);
	}

	private void createContextMenu(Control ctrl) {
		if (ctrl == null) return;

		contextMenu = new MenuManager("#Popup");
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = contextMenu.createContextMenu(ctrl);
		ctrl.setMenu(menu);
		getSite().registerContextMenu(contextMenu, null);
	}

	protected MenuManager getContextMenu() {
		return contextMenu;
	}

	protected void fillContextMenu(IMenuManager manager) {
		for (PartDecorator decorator: decorators) {
			decorator.contributeContextMenu(manager);
		}
	}

	protected IToolBarManager getToolBarManager() {
		IActionBars bars = getEditorSite().getActionBars();
		return bars.getToolBarManager();
	}

	protected void fillToolbar() {
		IToolBarManager mgr = getToolBarManager();

		for (PartDecorator decorator: decorators) {
			decorator.contributeToolbar(mgr);
		}

		mgr.update(true);
	}

}