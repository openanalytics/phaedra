package eu.openanalytics.phaedra.ui.link.importer.view;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.ui.util.folderbrowser.FolderBrowserFactory;

public class ImportSourceView extends ViewPart {

	private TreeViewer viewer;
	private DragSource treeDragSource;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(parent);
		
		viewer = FolderBrowserFactory.createBrowser(parent);
		
		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setLayout(new GridLayout());
		
		Transfer[] types = new Transfer[] {LocalSelectionTransfer.getTransfer()};
		int operations = DND.DROP_COPY;
		treeDragSource = new DragSource(viewer.getControl(), operations);
		treeDragSource.setTransfer(types);
		treeDragSource.addDragListener(new DragSourceAdapter() {
			public void dragSetData(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(viewer.getSelection());
			}
		});
		
		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewImportImportDestinationImportSource");
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

}
