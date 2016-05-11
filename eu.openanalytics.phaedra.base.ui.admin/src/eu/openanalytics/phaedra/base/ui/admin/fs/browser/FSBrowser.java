package eu.openanalytics.phaedra.base.ui.admin.fs.browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.admin.fs.AdminFileServer;
import eu.openanalytics.phaedra.base.ui.admin.fs.editor.PathEditorInput;
import eu.openanalytics.phaedra.base.ui.admin.fs.editor.SimpleEditor;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class FSBrowser extends ViewPart {

	private TreeViewer treeViewer;
	
	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new FSContentProvider());
		treeViewer.setLabelProvider(new FSLabelProvider());
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				TreeSelection selection = (TreeSelection)treeViewer.getSelection();
				File selectedFile = (File)selection.getFirstElement();

				if (selectedFile == null || selectedFile.isDirectory()) return;
				IPath location= new Path(selectedFile.getAbsolutePath());
				PathEditorInput input= new PathEditorInput(location);
				String editorId = SimpleEditor.class.getName();
				IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					page.openEditor(input, editorId);
				} catch (PartInitException ex) {}
			}
		});
		
		if (SecurityService.getInstance().isGlobalAdmin()) {
			treeViewer.setInput(new File(Screening.getEnvironment().getFileServer().getBasePath()));
		}
		
		createContextMenu();
		createToolbar();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewAdminFileServerBrowser");
	}

	@Override
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private void createToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(IconManager.getIconImage("refresh.png"));
				item.setToolTipText("Refresh the tree");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						treeViewer.refresh();
					}
				});
			}
		});
	}
	
	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		menuMgr.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("folder.png"));
				item.setText("New Folder");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!SecurityService.getInstance().isGlobalAdmin()) return;
						
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						File parent = (File)selection.getFirstElement();
						if (!parent.isDirectory()) return;
						
						InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), "New Folder",
								"Enter a name for the new folder", "New Folder", null);
						int retCode = dialog.open();
						if (retCode == Window.CANCEL) return;
						String name = dialog.getValue();
						
						try {
							AdminFileServer.getInstance().createDir(parent, name);
							treeViewer.refresh(parent);
						} catch (IOException ex) {
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Create error",
									"Failed to create folder " + name + ": " + ex.getMessage());
						}
					}
				});
				
				item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("file.png"));
				item.setText("New File");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!SecurityService.getInstance().isGlobalAdmin()) return;
						
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						File parent = (File)selection.getFirstElement();
						if (!parent.isDirectory()) return;
						
						InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), "New File",
								"Enter a name for the new file", "New File.txt", null);
						int retCode = dialog.open();
						if (retCode == Window.CANCEL) return;
						String name = dialog.getValue();
						
						try {
							AdminFileServer.getInstance().createFile(parent, name);
							treeViewer.refresh(parent);
						} catch (IOException ex) {
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Create error",
									"Failed to create file " + name + ": " + ex.getMessage());
						}
					}
				});
				
				item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("pencil.png"));
				item.setText("Edit File");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!SecurityService.getInstance().isGlobalAdmin()) return;
						
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						File selectedFile = (File)selection.getFirstElement();

						if (selectedFile == null || selectedFile.isDirectory()) return;
						IPath location= new Path(selectedFile.getAbsolutePath());
						PathEditorInput input= new PathEditorInput(location);
						String editorId = SimpleEditor.class.getName();
						IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						try {
							page.openEditor(input, editorId);
						} catch (PartInitException ex) {}
					}
				});
				
				item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("delete.png"));
				item.setText("Delete");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!SecurityService.getInstance().isGlobalAdmin()) return;

						List<File> filesToDelete = new ArrayList<File>();
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						for (Iterator<?> it = selection.iterator(); it.hasNext();) {
							filesToDelete.add((File)it.next());
						}
						
						boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete file(s)",
								"Are you sure you want to delete " + filesToDelete.size() + " file(s)?");
						
						if (confirmed) {
							for (File f: filesToDelete) {
								try {
									AdminFileServer.getInstance().deleteFile(f);
								} catch (IOException ex) {
									MessageDialog.openError(Display.getDefault().getActiveShell(), "Delete error",
											"Failed to delete item " + f.getName() + ": " + ex.getMessage());
									break;
								}
								treeViewer.refresh(f.getParentFile());
							}
						}
					}
				});
			}
		});
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}
}
