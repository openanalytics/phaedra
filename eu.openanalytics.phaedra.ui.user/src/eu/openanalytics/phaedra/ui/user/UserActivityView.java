package eu.openanalytics.phaedra.ui.user;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.model.user.util.UserActivity;
import eu.openanalytics.phaedra.model.user.util.UserActivitySorter;
import eu.openanalytics.phaedra.model.user.vo.User;
import eu.openanalytics.phaedra.model.user.vo.UserSession;

public class UserActivityView extends ViewPart {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	private TreeViewer treeViewer;

	@Override
	public void createPartControl(Composite parent) {

		FilteredTree filteredTree = new FilteredTree(parent
				, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, createFilter(), true);
		treeViewer = filteredTree.getViewer();
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		TreeColumn col = column.getColumn();
		col.setAlignment(SWT.LEFT);
		col.setText("Name");
		col.setToolTipText("User/Host name");
		col.setWidth(180);
		new ColumnViewerSorter<>(treeViewer, column, (o1, o2) -> {
			if (o1 instanceof UserActivity && o2 instanceof UserActivity) {
				UserActivity a1 = (UserActivity) o1;
				UserActivity a2 = (UserActivity) o2;
				return UserActivitySorter.USER_SORTER.compare(a1, a2);
			}
			if (o1 instanceof UserSession && o2 instanceof UserSession) {
				UserSession u1 = (UserSession) o1;
				UserSession u2 = (UserSession) o2;
				return u1.getHost().compareTo(u2.getHost());
			}
			return 0;
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("Version");
		col.setToolTipText("Phaedra Version");
		col.setWidth(140);
		new ColumnViewerSorter<>(treeViewer, column, (o1, o2) -> {
			if (o1 instanceof UserActivity && o2 instanceof UserActivity) {
				UserActivity a1 = (UserActivity) o1;
				UserActivity a2 = (UserActivity) o2;
				if (a1.isActive() && a2.isActive()) {
					List<UserSession> a1Sessions = a1.getActiveSessions();
					List<UserSession> a2Sessions = a2.getActiveSessions();
					String a1Version = a1Sessions.get(0).getVersion();
					String a2Version = a2Sessions.get(0).getVersion();
					return a1Version.compareTo(a2Version);
				}
			}
			if (o1 instanceof UserSession && o2 instanceof UserSession) {
				UserSession u1 = (UserSession) o1;
				UserSession u2 = (UserSession) o2;
				return u1.getVersion().compareTo(u2.getVersion());
			}
			return 0;
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("# of Logins");
		col.setToolTipText("Number of logins");
		col.setWidth(50);
		new ColumnViewerSorter<>(treeViewer, column, (o1, o2) -> {
			if (o1 instanceof UserActivity && o2 instanceof UserActivity) {
				UserActivity a1 = (UserActivity) o1;
				UserActivity a2 = (UserActivity) o2;
				return UserActivitySorter.LOGIN_COUNT_SORTER.compare(a1, a2);
			}
			//				if (o1 instanceof UserSession && o2 instanceof UserSession) {
			//					UserSession u1 = (UserSession) o1;
			//					UserSession u2 = (UserSession) o2;
			//					return u1.getVersion().compareTo(u2.getVersion());
			//				}
			return 0;
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("Last Login");
		col.setToolTipText("Last Login");
		col.setWidth(120);
		new ColumnViewerSorter<>(treeViewer, column, (o1, o2) -> {
			if (o1 instanceof UserActivity && o2 instanceof UserActivity) {
				UserActivity a1 = (UserActivity) o1;
				UserActivity a2 = (UserActivity) o2;
				return UserActivitySorter.ACTIVITY_SORTER.compare(a1, a2);
			}
			if (o1 instanceof UserSession && o2 instanceof UserSession) {
				UserSession u1 = (UserSession) o1;
				UserSession u2 = (UserSession) o2;
				return u1.getLoginDate().compareTo(u2.getLoginDate());
			}
			return 0;
		});

		treeViewer.setContentProvider(new ActivityContentProvider());
		treeViewer.setLabelProvider(new ActivityLabelProvider());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(treeViewer.getControl());

		createToolBar();
		loadActivity();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewUserActivity");
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	private void loadActivity() {
		JobUtils.runUserJob(monitor -> {
			final List<UserActivity> activities = UserService.getInstance().getAllUserActivity();
			if (monitor.isCanceled()) return;
			Collections.sort(activities, UserActivitySorter.ACTIVITY_SORTER);
			Display.getDefault().asyncExec(() -> {
				if (!treeViewer.getTree().isDisposed()) treeViewer.setInput(activities);
			});
		}, "Loading User Activity", IProgressMonitor.UNKNOWN, toString(), null);
	}

	private void createToolBar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		treeViewer.setData("isExpanded", false);
		Action expandAllAction = new Action() {
			@Override
			public void run() {
				if ((boolean) treeViewer.getData("isExpanded")) {
					treeViewer.collapseAll();
					treeViewer.setData("isExpanded", false);
				} else {
					treeViewer.expandAll();
					treeViewer.setData("isExpanded", true);
				}
			}
		};
		expandAllAction.setToolTipText("Collapse All");
		expandAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
		mgr.add(expandAllAction);

		Action refreshAction = new Action("Refresh") {
			@Override
			public void run() {
				loadActivity();
			}
		};
		refreshAction.setToolTipText("Refresh");
		refreshAction.setImageDescriptor(IconManager.getIconDescriptor("refresh.png"));
		mgr.add(refreshAction);
	}

	private PatternFilter createFilter() {
		return new PatternFilter() {
			// By default, the Filter uses a ILabelProvider. StyledCellLabelProvider is an IBaseLabelProvider and not a ILabelProvider implementation.
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element){
				if (element == null) {
					return false;
				}
				boolean matches = false;
				if (element instanceof UserActivity) {
					UserActivity activity = (UserActivity)element;
					matches = matches || wordMatches(activity.getUser().getUserName().toLowerCase());
					matches = matches || wordMatches(activity.getLoginCount() + "");
					matches = matches || wordMatches(DATE_FORMAT.format(activity.getUser().getLastLogon()));
				} else if (element instanceof UserSession) {
					UserSession session = (UserSession)element;
					matches = matches || wordMatches(session.getHost().toLowerCase());
					matches = matches || wordMatches(session.getVersion());
					matches = matches || wordMatches(DATE_FORMAT.format(session.getLoginDate()));
				}

				return matches;
			}
		};
	}

	public Styler getStyle(String version) {
		if (VersionUtils.isVersionUnknown(version)) {
			return new DefaultColorStyler(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY), null);
		} else {
			return new DefaultColorStyler(null, null);
		}
	}

	private class ActivityLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			StyledString styledString = null;

			Image icon = null;

			if (element instanceof UserActivity) {
				UserActivity activity = (UserActivity)element;
				if (cell.getColumnIndex() == 0) {
					styledString = new StyledString(activity.getUser().getUserName().toLowerCase());
					icon = activity.isActive() ? IconManager.getIconImage("status_green.png") : IconManager.getIconImage("status_grey.png");
				}
				if (cell.getColumnIndex() == 1) {
					List<UserSession> sessions = activity.getActiveSessions();
					String version = sessions.isEmpty() ? "" : sessions.get(0).getVersion();
					styledString = new StyledString(version, getStyle(version));
				}
				if (cell.getColumnIndex() == 2) {
					styledString = new StyledString(activity.getLoginCount() + "", StyledString.COUNTER_STYLER);
				}
				if (cell.getColumnIndex() == 3) {
					styledString = new StyledString(DATE_FORMAT.format(activity.getUser().getLastLogon()) + "", StyledString.DECORATIONS_STYLER);
				}
			} else if (element instanceof UserSession) {
				UserSession session = (UserSession)element;
				if (cell.getColumnIndex() == 0) {
					icon = IconManager.getIconImage("connect.png");
					styledString = new StyledString(session.getHost().toLowerCase() + "");
				}
				if (cell.getColumnIndex() == 1) {
					String version = session.getVersion();
					styledString = new StyledString(version, getStyle(version));
					//styledString = new StyledString(session.getVersion(), StyledString.COUNTER_STYLER);
				}
				if (cell.getColumnIndex() == 2) {
					styledString = new StyledString("");
				}
				if (cell.getColumnIndex() == 3) {
					styledString = new StyledString(DATE_FORMAT.format(session.getLoginDate()) + "", StyledString.DECORATIONS_STYLER);
				}
			}

			cell.setText(styledString.getString());
			cell.setStyleRanges(styledString.getStyleRanges());
			if (icon != null) cell.setImage(icon);
		}
	}

	private class ActivityContentProvider implements ITreeContentProvider {

		private List<UserActivity> root;

		@Override
		public void dispose() {
			// Do nothing.
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			root = (List<UserActivity>)inputElement;
			return root.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof UserActivity) {
				return ((UserActivity)parentElement).getActiveSessions().toArray();
			}
			else if (parentElement == root) {
				return root.toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof UserSession) {
				User user = ((UserSession)element).getUser();
				for (UserActivity act: root) {
					if (act.getUser() == user) return act;
				}
				return null;
			}
			else if (element instanceof UserActivity) {
				return root;
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof List<?>) {
				return true;
			} else if (element instanceof UserActivity) {
				return !((UserActivity)element).getActiveSessions().isEmpty();
			}
			return false;
		}
	}

	private class DefaultColorStyler extends Styler {

		private Color fForegroundColorName;
		private Color fBackgroundColorName;

		public DefaultColorStyler(Color foregroundColorName, Color backgroundColorName) {
			this.fForegroundColorName = foregroundColorName;
			this.fBackgroundColorName = backgroundColorName;
		}

		@Override
		public void applyStyles(TextStyle textStyle) {
			if (fForegroundColorName != null) textStyle.foreground = fForegroundColorName;
			if (fBackgroundColorName != null) textStyle.background = fBackgroundColorName;
		}
	}

}
