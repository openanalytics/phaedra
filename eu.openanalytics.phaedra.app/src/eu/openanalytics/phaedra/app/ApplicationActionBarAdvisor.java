package eu.openanalytics.phaedra.app;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import eu.openanalytics.phaedra.base.environment.EnvStatusBar;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.trafficlight.StatusManager;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();

		/* File menu */
		MenuManager menu = new MenuManager("File", IWorkbenchActionConstants.M_FILE);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator());
        menu.add(ActionFactory.QUIT.create(window));
		menuBar.add(menu);

		/* Edit menu */
		menu = new MenuManager("Edit", IWorkbenchActionConstants.M_EDIT);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(menu);

		/* Window menu */
		menu = new MenuManager("Window", IWorkbenchActionConstants.M_WINDOW);

        MenuManager changePerspMenuMgr = new MenuManager("Open Perspective", "openPerspective"); //$NON-NLS-1$
        IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);
        changePerspMenuMgr.add(changePerspMenuItem);
        menu.add(changePerspMenuMgr);

        MenuManager showViewMenuMgr = new MenuManager("Show View", "showView"); //$NON-NLS-1$
        IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
        showViewMenuMgr.add(showViewMenu);
        menu.add(showViewMenuMgr);
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        menu.add(new Separator());
        menu.add(ActionFactory.SAVE_PERSPECTIVE.create(window));
        menu.add(ActionFactory.RESET_PERSPECTIVE.create(window));
        menu.add(ActionFactory.CLOSE_PERSPECTIVE.create(window));
        menu.add(ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window));
		menu.add(new Separator());
        menu.add(ActionFactory.PREFERENCES.create(window));
		menuBar.add(menu);

		/* Tools menu */
		menu = new MenuManager("Tools", "toolsMenu");
		menu.add(new Separator("wizards"));
		Separator sep = new Separator("separator");
		sep.setVisible(true);
		menu.add(sep);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(menu);

		if (SecurityService.getInstance().isGlobalAdmin()) {
			/* Admin menu */
			menu = new MenuManager("Administrator", "adminMenu");
			menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			menuBar.add(menu);
		}

		/* Help menu */
		menu = new MenuManager("Help", IWorkbenchActionConstants.M_HELP);
		menu.add(ActionFactory.DYNAMIC_HELP.create(window));
		menu.add(ActionFactory.HELP_SEARCH.create(window));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator());
        menu.add(ActionFactory.ABOUT.create(window));
		menuBar.add(menu);

		super.fillMenuBar(menuBar);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		super.fillCoolBar(coolBar);
	}

	@Override
	protected void fillStatusLine(IStatusLineManager statusLine) {
		statusLine.add(new EnvStatusBar());
    	statusLine.add(StatusManager.getContributionItem());
    	statusLine.update(true);

    	super.fillStatusLine(statusLine);
	}

}
