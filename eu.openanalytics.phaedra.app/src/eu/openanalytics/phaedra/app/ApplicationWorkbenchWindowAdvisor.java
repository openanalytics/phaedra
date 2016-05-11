package eu.openanalytics.phaedra.app;

import javax.swing.UIManager;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import eu.openanalytics.phaedra.base.util.misc.VersionUtils;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    @Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
	public void preWindowOpen() {
    	IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 500));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setTitle(getTitle());
		configurer.setShowPerspectiveBar(true);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e){
			// Fallback to default look and feel
		}

		IPreferenceStore apiStore = PlatformUI.getPreferenceStore();
		apiStore.setValue(IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR, "TOP_RIGHT");
		apiStore.setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, "TRUE");
		apiStore.setDefault(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false);
		apiStore.setDefault(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);
    }

    @Override
    public void postWindowOpen() {
    	// Maximize window
    	getWindowConfigurer().getWindow().getShell().setMaximized(true);
    	
    	// Filter unwanted workbench contributions
    	getWindowConfigurer().getActionBarConfigurer().getMenuManager().remove("org.eclipse.ui.run");
    	IContributionItem menu = getWindowConfigurer().getActionBarConfigurer().getMenuManager().find("org.eclipse.ui.run");
		if (menu != null) {
			getWindowConfigurer().getActionBarConfigurer().getMenuManager().remove(menu);
			menu.setVisible(false);
		}
		
		String[] itemsToRemove = new String[] {
				"org.eclipse.ui.preferencePages.Workbench/org.eclipse.compare.internal.ComparePreferencePage"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.ContentTypes"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Editors"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Startup"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Workspace"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.net.NetPreferences"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.equinox.security.ui.category"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Views/org.eclipse.ui.preferencePages.ColorsAndFonts"
				, "org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Views/org.eclipse.ui.preferencePages.Decorators"
		};
		PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager( );
		for (String itemToRemove: itemsToRemove) {
			pm.remove(itemToRemove);
		}
    }

    private static String getTitle() {
    	String version = VersionUtils.getPhaedraVersion();
    	if (version.equals("Unknown")) version = "";
    	return "Phaedra " + version;
    }
}
