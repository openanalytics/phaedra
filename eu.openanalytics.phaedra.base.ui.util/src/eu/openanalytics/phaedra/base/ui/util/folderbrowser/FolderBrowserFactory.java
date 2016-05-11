package eu.openanalytics.phaedra.base.ui.util.folderbrowser;

import java.io.File;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider.FEContentProvider;
import eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider.FELabelProvider;
import eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider.TreeRoot;
import eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider.TreeSorter;

public class FolderBrowserFactory {

	public static TreeViewer createBrowser(Composite parent) {
		return createBrowser(parent, null, false);
	}
	
	public static TreeViewer createBrowser(Composite parent, File rootFolder) {
		return createBrowser(parent, rootFolder, false);
	}
	
	public static TreeViewer createBrowser(Composite parent, File rootFolder, boolean showFiles) {
		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new FEContentProvider(showFiles));
		viewer.setLabelProvider(new FELabelProvider());
		viewer.setSorter(new TreeSorter());
		viewer.setAutoExpandLevel(2);
		viewer.setInput(createRoot(rootFolder));
		return viewer;
	}
	
	public static TreeRoot createRoot(File rootFolder) {
		TreeRoot root = rootFolder == null ? new TreeRoot() : new TreeRoot(rootFolder);
		return root;
	}
}
