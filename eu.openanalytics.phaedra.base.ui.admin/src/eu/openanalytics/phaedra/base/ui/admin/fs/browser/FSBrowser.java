package eu.openanalytics.phaedra.base.ui.admin.fs.browser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.admin.fs.EditFSFileCmd;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

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
				String path = (String) selection.getFirstElement();
				if (path == null || FSContentProvider.isDirectory(path)) return;
				EditFSFileCmd.execute(path);
			}
		});
		
		if (SecurityService.getInstance().isGlobalAdmin()) treeViewer.setInput("/");
		
		createContextMenu();
		createToolbar();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewAdminFileServerBrowser");
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
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						String path = (String) selection.getFirstElement();
						if (path == null || !FSContentProvider.isDirectory(path)) return;
						
						InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), "New Folder",
								"Enter a name for the new folder", "New Folder", null);
						int retCode = dialog.open();
						if (retCode == Window.CANCEL) return;
						String name = dialog.getValue();
						
						try {
							createFile(path, name, true);
							treeViewer.refresh(path);
						} catch (IOException ex) {
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Create error",
									"Failed to create folder " + name + ": " + ex.getMessage());
						}
					}
				});
			}
		});
		menuMgr.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("file.png"));
				item.setText("New File");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						String path = (String) selection.getFirstElement();
						if (path == null || !FSContentProvider.isDirectory(path)) return;
						
						InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), "New File",
								"Enter a name for the new file", "New File.txt", null);
						int retCode = dialog.open();
						if (retCode == Window.CANCEL) return;
						String name = dialog.getValue();
						
						try {
							createFile(path, name, false);
							treeViewer.refresh(path);
						} catch (IOException ex) {
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Create error",
									"Failed to create file " + name + ": " + ex.getMessage());
						}
					}
				});
			}
		});
		menuMgr.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);	
				item.setImage(IconManager.getIconImage("pencil.png"));
				item.setText("Edit File");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						String path = (String) selection.getFirstElement();
						if (path == null || FSContentProvider.isDirectory(path)) return;
						EditFSFileCmd.execute(path);
					}
				});
			}
		});
		menuMgr.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setImage(IconManager.getIconImage("delete.png"));
				item.setText("Delete");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						List<String> filesToDelete = new ArrayList<>();
						TreeSelection selection = (TreeSelection)treeViewer.getSelection();
						for (Iterator<?> it = selection.iterator(); it.hasNext();) {
							filesToDelete.add((String)it.next());
						}
						
						boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete file(s)",
								"Are you sure you want to delete " + filesToDelete.size() + " file(s)?");
						
						if (confirmed) {
							for (String f: filesToDelete) {
								try {
									deleteFile(f);
								} catch (IOException ex) {
									MessageDialog.openError(Display.getDefault().getActiveShell(), "Delete error",
											"Failed to delete item " + f + ": " + ex.getMessage());
									break;
								}
								treeViewer.refresh(FileUtils.getPath(f));
							}
						}
					}
				});
			}
		});
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}
	
	private void createFile(String path, String name, boolean isDir) throws IOException {
		String fullPath = path + "/" + name;
		if (isDir) Screening.getEnvironment().getFileServer().mkDir(fullPath);
		else Screening.getEnvironment().getFileServer().putContents(fullPath, new byte[0]);
	}
	
	private void deleteFile(String path) throws IOException {
		Screening.getEnvironment().getFileServer().delete(path);
	}
}
