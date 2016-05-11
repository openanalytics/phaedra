package eu.openanalytics.phaedra.ui.link.importer;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eu.openanalytics.phaedra.ui.link.importer.view.ImportDestinationView;
import eu.openanalytics.phaedra.ui.link.importer.view.ImportSourceView;
import eu.openanalytics.phaedra.ui.link.importer.view.ImportView;

public class ImportPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout bottomFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, layout.getEditorArea());
		bottomFolder.addView("org.eclipse.ui.console.ConsoleView");
		
		IFolderLayout bottomRightFolder = layout.createFolder("bottomRight", IPageLayout.RIGHT, 0.6f, "bottom");
		bottomRightFolder.addView("org.eclipse.ui.views.ProgressView");
		
		IFolderLayout leftFolder = layout.createFolder("left", IPageLayout.LEFT, 0.2f, layout.getEditorArea());
		leftFolder.addView(ImportSourceView.class.getName());
		
		IFolderLayout rightFolder = layout.createFolder("right", IPageLayout.RIGHT, 0.7f, layout.getEditorArea());
		rightFolder.addView(ImportDestinationView.class.getName());

		IFolderLayout middleFolder = layout.createFolder("middle", IPageLayout.TOP, 0.9f, layout.getEditorArea());
		middleFolder.addView(ImportView.class.getName());
		
		layout.setEditorAreaVisible(false);
	}
}
