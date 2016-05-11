package eu.openanalytics.phaedra.app;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class DefaultPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout leftFolder = layout.createFolder("left", IPageLayout.LEFT, 0.2f, layout.getEditorArea());
		leftFolder.addView("eu.openanalytics.phaedra.base.ui.navigator.Navigator");
		
		IFolderLayout leftBottomFolder = layout.createFolder("leftBottom", IPageLayout.BOTTOM, 0.45f, "left");
		leftBottomFolder.addView("eu.openanalytics.phaedra.ui.plate.inspector.feature.FeatureInspector");
		
		IPlaceholderFolderLayout logPlaceholder = layout.createPlaceholderFolder("ph1", IPageLayout.BOTTOM, 0.75f, layout.getEditorArea());
		logPlaceholder.addPlaceholder("eu.openanalytics.phaedra.ui.link.importer.view.DataCaptureLogView");
		
		IPlaceholderFolderLayout placeholder = layout.createPlaceholderFolder("ph2", IPageLayout.RIGHT, 0.7f, layout.getEditorArea());
		placeholder.addPlaceholder("*");
	}
}
