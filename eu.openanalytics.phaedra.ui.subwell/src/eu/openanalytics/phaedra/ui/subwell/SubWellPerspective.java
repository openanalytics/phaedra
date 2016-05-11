package eu.openanalytics.phaedra.ui.subwell;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eu.openanalytics.phaedra.ui.subwell.chart.v2.view.SubWellScatter2DView;

public class SubWellPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		
		IFolderLayout leftFolder = layout.createFolder("left", IPageLayout.LEFT, 0.2f, layout.getEditorArea());
		leftFolder.addView("eu.openanalytics.phaedra.ui.navigator.Navigator");
		
		IFolderLayout bottomLeftFolder = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.45f, "left");
		bottomLeftFolder.addView("eu.openanalytics.phaedra.ui.plate.inspector.feature.FeatureInspector");

		IFolderLayout bottomFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.7f, layout.getEditorArea());
		bottomFolder.addView(SubWellDataView.class.getName());
		
		IFolderLayout rightFolder = layout.createFolder("right", IPageLayout.RIGHT, 0.6f, layout.getEditorArea());
		rightFolder.addView(SubWellScatter2DView.class.getName());
		rightFolder.addPlaceholder("*");
	}
}
